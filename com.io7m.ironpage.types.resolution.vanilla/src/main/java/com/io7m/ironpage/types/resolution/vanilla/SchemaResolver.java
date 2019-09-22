/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.ironpage.types.resolution.vanilla;

import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.types.api.SchemaDeclaration;
import com.io7m.ironpage.types.api.SchemaIdentifier;
import com.io7m.ironpage.types.api.SchemaName;
import com.io7m.ironpage.types.resolution.api.SchemaResolvedSet;
import com.io7m.ironpage.types.resolution.api.SchemaResolvedSetEdge;
import com.io7m.ironpage.types.resolution.api.SchemaResolverError;
import com.io7m.ironpage.types.resolution.api.SchemaResolverErrorReceiverType;
import com.io7m.ironpage.types.resolution.api.SchemaResolverType;
import com.io7m.ironpage.types.resolution.spi.SchemaDirectoryType;
import com.io7m.jaffirm.core.Invariants;
import io.vavr.collection.SortedMap;
import io.vavr.collection.Vector;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeMap;

/**
 * The default implementation of the {@link SchemaResolverType} interface.
 */

public final class SchemaResolver implements SchemaResolverType
{
  private static final Logger LOG = LoggerFactory.getLogger(SchemaResolver.class);

  private final Vector<SchemaDirectoryType> services;
  private final ResourceBundle resources;

  SchemaResolver(
    final Locale inLocale,
    final Vector<SchemaDirectoryType> inServices)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    final var locale =
      Objects.requireNonNull(inLocale, "locale");
    this.resources =
      ResourceBundle.getBundle("com.io7m.ironpage.types.resolution.vanilla.Resolution", locale);
  }

  private static void safeErrorPublish(
    final SchemaResolverErrorReceiverType receiver,
    final SchemaResolverError error)
  {
    try {
      receiver.receive(error);
    } catch (final Exception e) {
      LOG.error("ignored exception raised by error receiver: ", e);
    }
  }

  private void resolveImports(
    final SchemaResolverErrorReceiverType receiver,
    final SchemaResolution resolution,
    final Optional<SchemaDeclaration> requestingSchema,
    final SortedMap<SchemaName, SchemaIdentifier> imports)
  {
    for (final var schemaName : imports.keySet()) {
      final var schemaImported = imports.get(schemaName).get();

      /*
       * Find the schema.
       */

      final var schemaOpt = this.findSchema(receiver, schemaImported);
      if (schemaOpt.isEmpty()) {
        resolution.failed = true;
        safeErrorPublish(receiver, this.errorSchemaNotFound(requestingSchema, schemaImported));
        continue;
      }

      /*
       * Check to see if another version of the schema already exists
       * in the import graph.
       */

      final var schema = schemaOpt.get();
      final var resolvedId = schema.identifier();
      if (resolution.schemas.containsKey(schemaName)) {
        final var existingSchema = resolution.schemas.get(schemaName);
        final var existingId = existingSchema.identifier();
        if (!Objects.equals(existingId, resolvedId)) {
          resolution.failed = true;
          safeErrorPublish(
            receiver,
            this.errorVersionConflict(resolution, existingId, resolvedId));
          continue;
        }
      }

      resolution.schemas.put(schemaName, schema);
      resolution.schemaGraph.addVertex(resolvedId);
      LOG.trace("add vertex {}", resolvedId.show());

      /*
       * Check for cycles in the graph.
       */

      if (requestingSchema.isPresent()) {
        final var requestingId = requestingSchema.get().identifier();
        final var edge = SchemaResolvedSetEdge.of(requestingId, resolvedId);

        try {
          LOG.trace("add edge {} -> {}", requestingId.show(), resolvedId.show());
          resolution.schemaGraph.addEdge(requestingId, resolvedId, edge);
        } catch (final IllegalArgumentException e) {
          resolution.failed = true;
          safeErrorPublish(
            receiver,
            this.errorCircularImport(resolution, requestingId, resolvedId));
          continue;
        }
      }

      /*
       * Resolve all of the imports of the imported schema.
       */

      this.resolveImports(
        receiver,
        resolution,
        Optional.of(schema),
        schema.importsByName());
    }
  }

  private Optional<SchemaDeclaration> findSchema(
    final SchemaResolverErrorReceiverType receiver,
    final SchemaIdentifier schema)
  {
    for (final var service : this.services) {
      try {
        final var resultOpt = service.findSchema(schema);
        if (resultOpt.isPresent()) {
          final var result = resultOpt.get();
          Invariants.checkInvariant(
            result.identifier(),
            Objects.equals(result.identifier(), schema),
            schemaIdentifier -> "Returned schema identifier must match " + schema.show());
          return resultOpt;
        }
      } catch (final Exception e) {
        final var errorAttributes =
          io.vavr.collection.TreeMap.of(
            this.localize("schemaDirectory"),
            service.getClass().getCanonicalName());

        safeErrorPublish(
          receiver,
          SchemaResolverError.builder()
            .setMessage(this.localize("schemaDirectoryFailed"))
            .setAttributes(errorAttributes)
            .setSeverity(ErrorSeverity.SEVERITY_WARNING)
            .setErrorCode(SCHEMA_DIRECTORY_FAILED)
            .setException(e)
            .build());
      }
    }

    return Optional.empty();
  }

  private String localize(
    final String key)
  {
    return this.resources.getString(key);
  }

  @Override
  public Optional<SchemaResolvedSet> resolve(
    final SortedMap<SchemaName, SchemaIdentifier> imports,
    final SchemaResolverErrorReceiverType receiver)
  {
    Objects.requireNonNull(imports, "imports");
    Objects.requireNonNull(receiver, "receiver");

    final var resolution = new SchemaResolution();
    this.resolveImports(receiver, resolution, Optional.empty(), imports);

    if (resolution.failed) {
      return Optional.empty();
    }

    final var resolvedSet =
      SchemaResolvedSet.builder()
        .setGraph(resolution.schemaGraph)
        .setSchemas(Vector.ofAll(resolution.schemas.values()))
        .build();

    return Optional.of(resolvedSet);
  }

  private SchemaResolverError errorSchemaNotFound(
    final Optional<SchemaDeclaration> source,
    final SchemaIdentifier target)
  {
    final var errorAttributes = new TreeMap<String, String>();
    source.ifPresent(schemaDeclaration -> errorAttributes.put(
      this.localize("schemaSource"), schemaDeclaration.identifier().show()));
    errorAttributes.put(this.localize("schemaTarget"), target.show());

    return SchemaResolverError.builder()
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(SCHEMA_NOT_FOUND)
      .setMessage(this.localize("schemaNotFound"))
      .setAttributes(io.vavr.collection.TreeMap.ofAll(errorAttributes))
      .build();
  }

  private SchemaResolverError errorVersionConflict(
    final SchemaResolution resolution,
    final SchemaIdentifier existingId,
    final SchemaIdentifier resolvedId)
  {
    final var errorAttributes =
      io.vavr.collection.TreeMap.of(
        this.localize("schemaConflictingExisting"), existingId.show(),
        this.localize("schemaConflictingResolved"), resolvedId.show());

    final var errorBuilder = SchemaResolverError.builder();
    final var existingImports = resolution.schemaGraph.incomingEdgesOf(existingId);
    if (!existingImports.isEmpty()) {
      for (final var edge : existingImports) {
        errorBuilder.addMessageExtras(this.formatImport(edge, existingId));
      }
    }

    return errorBuilder
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setMessage(this.localize("schemaVersionConflict"))
      .setErrorCode(VERSION_CONFLICT)
      .setAttributes(errorAttributes)
      .build();
  }

  private SchemaResolverError errorCircularImport(
    final SchemaResolution resolution,
    final SchemaIdentifier requestingId,
    final SchemaIdentifier resolvedId)
  {
    final var errorBuilder = SchemaResolverError.builder();

    /*
     * Because a cycle as occurred on an insertion of edge A → B, then
     * there must be some path B → A already in the graph. Use a
     * shortest path algorithm to determine that path.
     */

    final var shortestPath =
      new DijkstraShortestPath<>(resolution.schemaGraph);
    final List<SchemaResolvedSetEdge> edges =
      new ArrayList<>(shortestPath.getPath(resolvedId, requestingId).getEdgeList());

    edges.add(SchemaResolvedSetEdge.of(requestingId, resolvedId));

    errorBuilder.addMessageExtras(this.localize("schemaImportCycleDetail"));
    for (final var edge : edges) {
      errorBuilder.addMessageExtras(this.formatImport(edge, edge.target()));
    }

    return errorBuilder
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setMessage(this.localize("schemaImportCycle"))
      .setErrorCode(CIRCULAR_IMPORT)
      .build();
  }

  private String formatImport(
    final SchemaResolvedSetEdge edge,
    final SchemaIdentifier target)
  {
    return MessageFormat.format(
      this.localize("schemaImports"),
      edge.source().show(),
      target.show());
  }

  private static final class SchemaResolution
  {
    private final HashMap<SchemaName, SchemaDeclaration> schemas;
    private final DirectedAcyclicGraph<SchemaIdentifier, SchemaResolvedSetEdge> schemaGraph;
    private boolean failed;

    SchemaResolution()
    {
      this.schemas = new HashMap<>();
      this.schemaGraph = new DirectedAcyclicGraph<>(SchemaResolvedSetEdge.class);
      this.failed = false;
    }
  }
}
