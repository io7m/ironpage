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
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaName;
import com.io7m.ironpage.presentable.api.PresentableAttributes;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolvedSet;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolvedSetEdge;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverError;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverErrorReceiverType;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverType;
import com.io7m.ironpage.types.resolution.spi.MetaSchemaDirectoryType;
import com.io7m.jaffirm.core.Invariants;
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
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The default implementation of the {@link MetaSchemaResolverType} interface.
 */

public final class MetaSchemaResolver implements MetaSchemaResolverType
{
  private static final Logger LOG = LoggerFactory.getLogger(MetaSchemaResolver.class);

  private final List<MetaSchemaDirectoryType> services;
  private final ResourceBundle resources;

  MetaSchemaResolver(
    final Locale inLocale,
    final List<MetaSchemaDirectoryType> inServices)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    final var locale =
      Objects.requireNonNull(inLocale, "locale");
    this.resources =
      ResourceBundle.getBundle("com.io7m.ironpage.types.resolution.vanilla.Resolution", locale);
  }

  private static void safeErrorPublish(
    final MetaSchemaResolverErrorReceiverType receiver,
    final MetaSchemaResolverError error)
  {
    try {
      receiver.receive(error);
    } catch (final Exception e) {
      LOG.error("ignored exception raised by error receiver: ", e);
    }
  }

  private void resolveImports(
    final MetaSchemaResolverErrorReceiverType receiver,
    final SchemaResolution resolution,
    final Optional<MetaSchema> requestingSchema,
    final SortedMap<MetaSchemaName, MetaSchemaIdentifier> imports)
  {
    for (final var entry : imports.entrySet()) {
      final var schemaName = entry.getKey();
      final var schemaImported = imports.get(schemaName);

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
        final var edge = MetaSchemaResolvedSetEdge.of(requestingId, resolvedId);

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

  private Optional<MetaSchema> findSchema(
    final MetaSchemaResolverErrorReceiverType receiver,
    final MetaSchemaIdentifier schema)
  {
    for (final var service : this.services) {
      try {
        final var resultOpt = service.findSchema(schema);
        if (resultOpt.isPresent()) {
          final var result = resultOpt.get();
          final var schemaId = result.identifier();
          Invariants.checkInvariant(
            schemaId,
            Objects.equals(schemaId, schema),
            schemaIdentifier -> "Returned schema identifier must match " + schema.show());
          return resultOpt;
        }
      } catch (final Exception e) {
        final var errorAttributes =
          PresentableAttributes.one(
            this.localize("schemaDirectory"),
            service.getClass().getCanonicalName());

        safeErrorPublish(
          receiver,
          MetaSchemaResolverError.builder()
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
  public Optional<MetaSchemaResolvedSet> resolve(
    final SortedMap<MetaSchemaName, MetaSchemaIdentifier> imports,
    final MetaSchemaResolverErrorReceiverType receiver)
  {
    Objects.requireNonNull(imports, "imports");
    Objects.requireNonNull(receiver, "receiver");

    final var resolution = new SchemaResolution();
    this.resolveImports(receiver, resolution, Optional.empty(), imports);

    if (resolution.failed) {
      return Optional.empty();
    }

    final var resolvedSet =
      MetaSchemaResolvedSet.builder()
        .setGraph(resolution.schemaGraph)
        .setSchemas(resolution.schemas.values())
        .build();

    return Optional.of(resolvedSet);
  }

  private MetaSchemaResolverError errorSchemaNotFound(
    final Optional<MetaSchema> source,
    final MetaSchemaIdentifier target)
  {
    final var errorAttributes = new TreeMap<String, String>();
    source.ifPresent(schemaDeclaration -> errorAttributes.put(
      this.localize("schemaSource"), schemaDeclaration.identifier().show()));
    errorAttributes.put(this.localize("schemaTarget"), target.show());

    return MetaSchemaResolverError.builder()
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(SCHEMA_NOT_FOUND)
      .setMessage(this.localize("schemaNotFound"))
      .setAttributes(errorAttributes)
      .build();
  }

  private MetaSchemaResolverError errorVersionConflict(
    final SchemaResolution resolution,
    final MetaSchemaIdentifier existingId,
    final MetaSchemaIdentifier resolvedId)
  {
    final var errorAttributes =
      PresentableAttributes.of(
        PresentableAttributes.entry(this.localize("schemaConflictingExisting"), existingId.show()),
        PresentableAttributes.entry(this.localize("schemaConflictingResolved"), resolvedId.show()));

    final var errorBuilder = MetaSchemaResolverError.builder();
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

  private MetaSchemaResolverError errorCircularImport(
    final SchemaResolution resolution,
    final MetaSchemaIdentifier requestingId,
    final MetaSchemaIdentifier resolvedId)
  {
    final var errorBuilder = MetaSchemaResolverError.builder();

    /*
     * Because a cycle as occurred on an insertion of edge A → B, then
     * there must be some path B → A already in the graph. Use a
     * shortest path algorithm to determine that path.
     */

    final var shortestPath =
      new DijkstraShortestPath<>(resolution.schemaGraph);
    final List<MetaSchemaResolvedSetEdge> edges =
      new ArrayList<>(shortestPath.getPath(resolvedId, requestingId).getEdgeList());

    edges.add(MetaSchemaResolvedSetEdge.of(requestingId, resolvedId));

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
    final MetaSchemaResolvedSetEdge edge,
    final MetaSchemaIdentifier target)
  {
    return MessageFormat.format(
      this.localize("schemaImports"),
      edge.source().show(),
      target.show());
  }

  private static final class SchemaResolution
  {
    private final HashMap<MetaSchemaName, MetaSchema> schemas;
    private final DirectedAcyclicGraph<MetaSchemaIdentifier, MetaSchemaResolvedSetEdge> schemaGraph;
    private boolean failed;

    SchemaResolution()
    {
      this.schemas = new HashMap<>();
      this.schemaGraph = new DirectedAcyclicGraph<>(MetaSchemaResolvedSetEdge.class);
      this.failed = false;
    }
  }
}
