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


package com.io7m.ironpage.metadata.schema.compiler.vanilla;

import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.metadata.schema.ast.MADeclAttribute;
import com.io7m.ironpage.metadata.schema.ast.MADeclImport;
import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.ast.MADeclType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerError;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.typed.MSCVSafeErrorConsumer;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.jaffirm.core.Invariants;
import com.io7m.jlexing.core.LexicalPositions;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class MSCVCompiler implements MSchemaCompilerType
{
  private final MSCVCompilers compilers;
  private final MSchemaCompilerErrorConsumerType errors;
  private final MSchemaCompilerMessagesType messages;
  private final MSchemaCompilerLoaderType loader;
  private final URI uri;
  private final InputStream stream;

  MSCVCompiler(
    final MSCVCompilers inCompilers,
    final MSchemaCompilerErrorConsumerType inErrors,
    final MSchemaCompilerMessagesType inMessages,
    final MSchemaCompilerLoaderType inLoader,
    final URI inUri,
    final InputStream inStream)
  {
    this.compilers =
      Objects.requireNonNull(inCompilers, "compilers");
    this.errors =
      new MSCVSafeErrorConsumer(Objects.requireNonNull(inErrors, "errors"));
    this.messages =
      Objects.requireNonNull(inMessages, "messages");
    this.loader =
      Objects.requireNonNull(inLoader, "loader");
    this.uri =
      Objects.requireNonNull(inUri, "uri");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
  }

  private static void checkInvariants(
    final MADeclSchema<?> before,
    final MADeclSchema<?> after)
  {
    final Map<String, ? extends MADeclAttribute<?>> afterAttributes = after.attributes();
    final var afterAttrsSize = afterAttributes.size();
    final Map<String, ? extends MADeclAttribute<?>> beforeAttributes = before.attributes();
    final var beforeAttrsSize = beforeAttributes.size();

    Invariants.checkInvariantV(
      Integer.valueOf(afterAttrsSize),
      afterAttrsSize == beforeAttrsSize,
      "Before attribute count %d must match after attribute count %d",
      Integer.valueOf(beforeAttrsSize),
      Integer.valueOf(afterAttrsSize));

    final Map<String, ? extends MADeclType<?>> afterTypes = after.types();
    final var afterTypesSize = afterTypes.size();
    final Map<String, ? extends MADeclType<?>> beforeTypes = before.types();
    final var beforeTypesSize = beforeTypes.size();

    Invariants.checkInvariantV(
      Integer.valueOf(afterTypesSize),
      afterTypesSize == beforeTypesSize,
      "Before types count %d must match after types count %d",
      Integer.valueOf(beforeTypesSize),
      Integer.valueOf(afterTypesSize));

    final List<? extends MADeclImport<?>> afterImports = after.imports();
    final var afterImportsSize = afterImports.size();
    final List<? extends MADeclImport<?>> beforeImports = before.imports();
    final var beforeImportsSize = beforeImports.size();

    Invariants.checkInvariantV(
      Integer.valueOf(afterImportsSize),
      afterImportsSize == beforeImportsSize,
      "Before imports count %d must match after imports count %d",
      Integer.valueOf(beforeImportsSize),
      Integer.valueOf(afterImportsSize));
  }

  @Override
  public Optional<MetaSchema> execute()
  {
    final var parsers = this.compilers.parsers();
    try (var parser = parsers.createParser(this.errors, this.messages, this.uri, this.stream)) {
      final var parsedOpt = parser.execute();
      if (parsedOpt.isEmpty()) {
        return Optional.empty();
      }
      final var binders = this.compilers.binders();
      final var parsed = parsedOpt.get();
      try (var binder = binders.createBinder(
        this.errors, this.loader, this.messages, this.uri, parsed)) {
        final var boundOpt = binder.execute();
        if (boundOpt.isEmpty()) {
          return Optional.empty();
        }

        final var typers = this.compilers.typers();
        final var bound = boundOpt.get();
        checkInvariants(parsed, bound);

        try (var typer = typers.createTyper(
          this.errors, this.loader, this.messages, this.uri, bound)) {
          return typer.execute();
        }
      }
    } catch (final Exception e) {
      this.errors.receive(this.errorInternal(e));
      return Optional.empty();
    }
  }

  private MSchemaCompilerError errorInternal(final Exception e)
  {
    return MSchemaCompilerError.builder()
      .setErrorCode(INTERNAL_ERROR)
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setMessage(this.messages.format("errorInternal"))
      .setException(e)
      .setLexical(LexicalPositions.zero())
      .build();
  }

  @Override
  public void close()
    throws Exception
  {
    this.stream.close();
  }
}
