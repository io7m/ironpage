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

package com.io7m.ironpage.metadata.schema.compiler.vanilla.loader;

import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerError;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerProviderType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.spi.MSchemaCompilerSourceType;
import com.io7m.ironpage.metadata.schema.compiler.spi.MSchemaCompilerStream;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.typed.MSCVSafeErrorConsumer;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.presentable.api.PresentableAttributes;
import com.io7m.jaffirm.core.Invariants;
import com.io7m.jlexing.core.LexicalPositions;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

final class MSCVLoader implements MSchemaCompilerLoaderType
{
  private static final Logger LOG = LoggerFactory.getLogger(MSCVLoader.class);

  private final MSchemaCompilerErrorConsumerType errors;
  private final MSchemaCompilerProviderType compilers;
  private final HashMap<MetaSchemaIdentifier, MetaSchema> schemas;
  private final DirectedAcyclicGraph<MetaSchemaIdentifier, SchemaEdge> schemaGraph;
  private final MSchemaCompilerMessagesType messages;
  private final MSchemaCompilerSourceType sources;

  MSCVLoader(
    final MSchemaCompilerProviderType inCompilers,
    final MSchemaCompilerErrorConsumerType inErrors,
    final MSchemaCompilerMessagesType inMessages,
    final MSchemaCompilerSourceType inSources)
  {
    this.compilers =
      Objects.requireNonNull(inCompilers, "compilers");
    this.errors =
      new MSCVSafeErrorConsumer(Objects.requireNonNull(inErrors, "errors"));
    this.messages =
      Objects.requireNonNull(inMessages, "messages");
    this.sources =
      Objects.requireNonNull(inSources, "sources");
    this.schemas =
      new HashMap<>(16);
    this.schemaGraph =
      new DirectedAcyclicGraph<>(SchemaEdge.class);
  }

  @Override
  public Optional<MetaSchema> load(
    final MetaSchemaIdentifier requesterId,
    final MetaSchemaIdentifier schemaId)
  {
    Objects.requireNonNull(requesterId, "requester");
    Objects.requireNonNull(schemaId, "schema");

    if (this.schemas.containsKey(schemaId)) {
      LOG.trace("returning {} from cache", schemaId.show());
      return Optional.of(this.schemas.get(schemaId));
    }

    try {
      this.schemaGraph.addVertex(requesterId);
      this.schemaGraph.addVertex(schemaId);
      this.schemaGraph.addEdge(requesterId, schemaId, new SchemaEdge(requesterId, schemaId));
    } catch (final IllegalArgumentException e) {
      this.errors.receive(this.errorCyclicImport(requesterId, schemaId, e));
      this.schemaGraph.removeVertex(schemaId);
      return Optional.empty();
    }

    return this.openStream(requesterId, schemaId).flatMap(stream -> {
      final var streamId = stream.identifier();
      Invariants.checkInvariantV(
        schemaId,
        Objects.equals(streamId, schemaId),
        "Loaded schema stream must have an ID of %s (received %s)",
        schemaId,
        streamId);
      return this.compileSchema(requesterId, stream).flatMap(compiledSchema -> {
        final var compiledId = compiledSchema.identifier();
        Invariants.checkInvariantV(
          schemaId,
          Objects.equals(compiledId, schemaId),
          "Compiled schema must have an ID of %s (received %s)",
          schemaId,
          compiledId);
        return this.registerSchema(requesterId, compiledSchema);
      });
    });
  }

  private Optional<? extends MetaSchema> registerSchema(
    final MetaSchemaIdentifier requesterId,
    final MetaSchema compiledSchema)
  {
    final var compiledId = compiledSchema.identifier();

    try {
      this.schemas.put(compiledId, compiledSchema);
      return Optional.of(compiledSchema);
    } catch (final IllegalArgumentException e) {
      this.errors.receive(this.errorCyclicImport(requesterId, compiledId, e));
      return Optional.empty();
    }
  }

  private Optional<MSchemaCompilerStream> openStream(
    final MetaSchemaIdentifier requesterId,
    final MetaSchemaIdentifier schemaId)
  {
    try {
      LOG.trace("opening {} from sources", schemaId.show());
      return this.sources.openSchemaSource(schemaId);
    } catch (final IOException e) {
      this.errors.receive(this.errorSchemaSourceIO(requesterId, schemaId, e));
      return Optional.empty();
    }
  }

  private Optional<MetaSchema> compileSchema(
    final MetaSchemaIdentifier requesterId,
    final MSchemaCompilerStream compilerStream)
  {
    LOG.trace("compiling {}", compilerStream.identifier().show());

    try (var input = compilerStream.stream()) {
      try (var compiler = this.compilers.createCompiler(
        this.errors,
        this.messages,
        this,
        compilerStream.uri(),
        input)) {
        return compiler.execute();
      }
    } catch (final Exception e) {
      this.errors.receive(this.errorSchemaSourceIO(requesterId, compilerStream.identifier(), e));
      return Optional.empty();
    }
  }

  private MSchemaCompilerError errorSchemaSourceIO(
    final MetaSchemaIdentifier requester,
    final MetaSchemaIdentifier schema,
    final Exception exception)
  {
    final var errorAttributes = PresentableAttributes.of(
      PresentableAttributes.entry(this.messages.format("schema"), requester.show()),
      PresentableAttributes.entry(this.messages.format("schemaTarget"), schema.show())
    );

    return MSchemaCompilerError.builder()
      .setLexical(LexicalPositions.zero())
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(SOURCE_ERROR)
      .setAttributes(errorAttributes)
      .setMessage(this.messages.format("errorSource"))
      .setException(exception)
      .build();
  }

  private MSchemaCompilerError errorCyclicImport(
    final MetaSchemaIdentifier requester,
    final MetaSchemaIdentifier schema,
    final IllegalArgumentException exception)
  {
    final var dijkstra = new DijkstraShortestPath<>(this.schemaGraph);
    final var path = dijkstra.getPath(schema, requester);

    final var extras = new ArrayList<String>();
    extras.add(this.messages.format("cyclicImportPath"));
    extras.add(String.format("  %16s →", requester.show()));
    if (path != null) {
      for (final var edge : path.getEdgeList()) {
        extras.add(String.format("  %16s → %16s", edge.from.show(), edge.to.show()));
      }
    }
    extras.add(String.format("  → %16s", schema.show()));

    final var errorAttributes = PresentableAttributes.of(
      PresentableAttributes.entry(this.messages.format("schema"), requester.show()),
      PresentableAttributes.entry(this.messages.format("schemaTarget"), schema.show())
    );

    return MSchemaCompilerError.builder()
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(CYCLIC_IMPORT)
      .setAttributes(errorAttributes)
      .setMessageExtras(extras)
      .setMessage(this.messages.format("errorCyclicImport"))
      .setException(exception)
      .setLexical(LexicalPositions.zero())
      .build();
  }

  private static final class SchemaEdge
  {
    private final MetaSchemaIdentifier from;
    private final MetaSchemaIdentifier to;

    @Override
    public boolean equals(final Object o)
    {
      if (this == o) {
        return true;
      }
      if (o == null || !Objects.equals(this.getClass(), o.getClass())) {
        return false;
      }
      final var that = (SchemaEdge) o;
      return this.from.equals(that.from) && this.to.equals(that.to);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(this.from, this.to);
    }

    SchemaEdge(
      final MetaSchemaIdentifier inFrom,
      final MetaSchemaIdentifier inTo)
    {
      this.from = Objects.requireNonNull(inFrom, "from");
      this.to = Objects.requireNonNull(inTo, "to");
    }
  }
}
