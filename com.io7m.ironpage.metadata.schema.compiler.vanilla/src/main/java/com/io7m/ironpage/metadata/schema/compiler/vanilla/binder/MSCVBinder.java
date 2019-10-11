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

package com.io7m.ironpage.metadata.schema.compiler.vanilla.binder;

import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.metadata.schema.ast.MADeclAttribute;
import com.io7m.ironpage.metadata.schema.ast.MADeclComment;
import com.io7m.ironpage.metadata.schema.ast.MADeclImport;
import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.ast.MADeclType;
import com.io7m.ironpage.metadata.schema.ast.MAElementType;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferenceNamed;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferencePrimitive;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferenceType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerError;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaBoundType;
import com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaCompilerBinderType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.Parsed;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.typed.MSCVSafeErrorConsumer;
import com.io7m.ironpage.metadata.schema.types.api.AttributeName;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaName;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaNames;
import com.io7m.ironpage.metadata.schema.types.api.NameType;
import com.io7m.ironpage.metadata.schema.types.api.TypeName;
import com.io7m.ironpage.metadata.schema.types.api.TypeNameQualified;
import com.io7m.ironpage.presentable.api.PresentableAttributes;
import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

final class MSCVBinder implements MSchemaCompilerBinderType
{
  private static final Logger LOG = LoggerFactory.getLogger(MSCVBinder.class);

  private final MADeclSchema<Parsed> schema;
  private final MSchemaCompilerLoaderType loader;
  private final MSchemaCompilerMessagesType messages;
  private final MSCVSafeErrorConsumer errors;
  private final URI uri;

  MSCVBinder(
    final MSchemaCompilerErrorConsumerType inErrors,
    final MSchemaCompilerLoaderType inLoader,
    final MSchemaCompilerMessagesType inMessages,
    final URI inUri,
    final MADeclSchema<Parsed> inSchema)
  {
    this.errors =
      new MSCVSafeErrorConsumer(Objects.requireNonNull(inErrors, "errors"));
    this.loader =
      Objects.requireNonNull(inLoader, "loader");
    this.messages =
      Objects.requireNonNull(inMessages, "inMessages");
    this.uri =
      Objects.requireNonNull(inUri, "uri");
    this.schema =
      Objects.requireNonNull(inSchema, "schema");
  }

  private static Optional<MADeclImport<MSchemaBoundType>> findImportFor(
    final List<MADeclImport<MSchemaBoundType>> imports,
    final String schemaName)
  {
    return imports.stream()
      .filter(i -> Objects.equals(i.schemaName(), schemaName))
      .findFirst();
  }

  private static Optional<MADeclType<MSchemaBoundType>> findType(
    final List<MADeclType<MSchemaBoundType>> typeDecls,
    final String typeName)
  {
    return typeDecls.stream()
      .filter(t -> Objects.equals(t.name(), typeName))
      .findFirst();
  }

  private static Optional<MADeclComment<MSchemaBoundType>> bindComment(
    final MADeclComment<Parsed> element)
  {
    return Optional.of(
      MADeclComment.<MSchemaBoundType>builder()
        .setData(new BoundNothing())
        .setLexical(element.lexical())
        .setText(element.text())
        .build());
  }

  private static Optional<MATypeReferencePrimitive<MSchemaBoundType>> bindTypeReferencePrimitive(
    final MATypeReferencePrimitive<Parsed> element)
  {
    return Optional.of(
      MATypeReferencePrimitive.<MSchemaBoundType>builder()
        .setPrimitive(element.primitive())
        .setLexical(element.lexical())
        .setData(new BoundNothing())
        .build());
  }

  @Override
  public void close()
  {

  }

  @Override
  public Optional<MADeclSchema<MSchemaBoundType>> execute()
  {
    LOG.debug("executing binder for {}", this.uri);

    final var schemaIdentifierOpt =
      this.bindSchemaIdentifier(
        this.schema.lexical(),
        this.schema.schemaName(),
        this.schema.versionMajor(),
        this.schema.versionMinor());

    return schemaIdentifierOpt.flatMap(currentSchemaId -> {
      final var declarations = this.bindDeclarations(currentSchemaId);

      if (this.errors.isFailed()) {
        return Optional.empty();
      }

      final var resultSchema =
        MADeclSchema.<MSchemaBoundType>builder()
          .setData(new Bound(currentSchemaId))
          .setDeclarations(declarations)
          .setLexical(this.schema.lexical())
          .setSchemaName(currentSchemaId.name().name())
          .setVersionMajor(currentSchemaId.versionMajor())
          .setVersionMinor(currentSchemaId.versionMinor())
          .build();

      final var resultName = resultSchema.schemaName();
      Postconditions.checkPostcondition(
        resultName,
        MetaSchemaNames.isValidName(resultName),
        name -> "Schema name must be valid");

      return Optional.of(resultSchema);
    });
  }

  private List<MAElementType<MSchemaBoundType>> bindDeclarations(
    final MetaSchemaIdentifier currentSchemaId)
  {
    /*
     * Process comments first. There will only, in practice, be one top-level comment.
     */

    final var comments = new ArrayList<MADeclComment<MSchemaBoundType>>();
    for (final var decl : this.schema.declarations()) {
      if (decl instanceof MADeclComment) {
        final var result = bindComment((MADeclComment<Parsed>) decl);
        result.ifPresent(comments::add);
      }
    }

    /*
     * Process imports, because imports are needed to resolve types and attributes.
     */

    final var imports = new ArrayList<MADeclImport<MSchemaBoundType>>();
    for (final var decl : this.schema.declarations()) {
      if (decl instanceof MADeclImport) {
        final var result = this.bindImport(currentSchemaId, (MADeclImport<Parsed>) decl);
        result.ifPresent(imports::add);
      }
    }

    /*
     * Process types, because types are needed to resolve attributes.
     */

    final var types = new ArrayList<MADeclType<MSchemaBoundType>>();
    for (final var decl : this.schema.declarations()) {
      if (decl instanceof MADeclType) {
        final var result =
          this.bindTypeDeclaration(currentSchemaId, imports, types, (MADeclType<Parsed>) decl);
        result.ifPresent(types::add);
      }
    }

    /*
     * Process attributes.
     */

    final var attributes = new ArrayList<MADeclAttribute<MSchemaBoundType>>();
    for (final var decl : this.schema.declarations()) {
      if (decl instanceof MADeclAttribute) {
        final var result =
          this.bindAttribute(currentSchemaId, imports, types, (MADeclAttribute<Parsed>) decl);
        result.ifPresent(attributes::add);
      }
    }

    /*
     * Collect results. This has the effect of normalizing the order of declarations: Optional
     * comment, followed by imports, followed by types, followed by attributes.
     */

    final var capacity = imports.size() + types.size() + attributes.size();
    final var combined = new ArrayList<MAElementType<MSchemaBoundType>>(capacity);
    combined.addAll(comments);
    combined.addAll(imports);
    combined.addAll(types);
    combined.addAll(attributes);
    return List.copyOf(combined);
  }

  /**
   * Bind an import declaration by loading the target schema.
   */

  private Optional<MADeclImport<MSchemaBoundType>> bindImport(
    final MetaSchemaIdentifier currentSchema,
    final MADeclImport<Parsed> importDecl)
  {
    LOG.trace("binding import: {}", importDecl.schemaName());

    final var identifierOpt =
      this.bindSchemaIdentifier(
        importDecl.lexical(),
        importDecl.schemaName(),
        importDecl.versionMajor(),
        importDecl.versionMinor());

    return identifierOpt.flatMap(schemaIdentifier -> {
      return this.bindSchemaLoad(importDecl.lexical(), currentSchema, schemaIdentifier)
        .flatMap(loaded -> {
          return Optional.of(
            MADeclImport.<MSchemaBoundType>builder()
              .setLexical(importDecl.lexical())
              .setVersionMinor(schemaIdentifier.versionMinor())
              .setVersionMajor(schemaIdentifier.versionMajor())
              .setSchemaName(schemaIdentifier.name().name())
              .setData(new Bound(schemaIdentifier))
              .build());
        });
    });
  }

  private Optional<MetaSchema> bindSchemaLoad(
    final LexicalPosition<URI> lexical,
    final MetaSchemaIdentifier currentSchema,
    final MetaSchemaIdentifier schemaIdentifier)
  {
    final var loadedSchemaOpt = this.loadSchema(currentSchema, schemaIdentifier);
    if (loadedSchemaOpt.isEmpty()) {
      final var rawName = schemaIdentifier.name().name();
      this.errors.receive(this.errorNonexistentSchema(lexical, rawName));
    }
    return loadedSchemaOpt;
  }

  private Optional<MetaSchemaIdentifier> bindSchemaIdentifier(
    final LexicalPosition<URI> lexical,
    final String name,
    final BigInteger versionMajor,
    final BigInteger versionMinor)
  {
    try {
      return Optional.of(
        MetaSchemaIdentifier.of(MetaSchemaName.of(name), versionMajor, versionMinor));
    } catch (final Exception e) {
      this.errors.receive(this.errorSchemaNameInvalid(lexical, name, e));
      return Optional.empty();
    }
  }

  /**
   * Bind a type declaration by checking the validity of the name, and resolving the type reference.
   * Note that type references to types in this schema are resolved against the type declarations
   * that have been processed so far: This has the effect of preventing cyclic definitions because
   * it's simply not possible for a type "later" in a cycle to refer to a type "earlier" in a
   * cycle.
   */

  private Optional<MADeclType<MSchemaBoundType>> bindTypeDeclaration(
    final MetaSchemaIdentifier currentSchema,
    final List<MADeclImport<MSchemaBoundType>> imports,
    final List<MADeclType<MSchemaBoundType>> typeDecls,
    final MADeclType<Parsed> element)
  {
    LOG.trace("binding type declaration: {}", element.name());

    final var typeRefOpt =
      this.bindTypeReference(currentSchema, imports, typeDecls, element.baseType());
    final var typeNameOpt =
      this.bindTypeName(element.lexical(), element.name());

    return typeRefOpt.flatMap(typeRef -> {
      return typeNameOpt.flatMap(typeName -> {
        final var commentOpt = element.comment().flatMap(MSCVBinder::bindComment);

        return Optional.of(
          MADeclType.<MSchemaBoundType>builder()
            .setBaseType(typeRef)
            .setComment(commentOpt)
            .setData(new Bound(typeName))
            .setLexical(element.lexical())
            .setName(typeName.name())
            .build());
      });
    });
  }

  private Optional<TypeName> bindTypeName(
    final LexicalPosition<URI> lexical,
    final String typeName)
  {
    try {
      return Optional.of(TypeName.of(typeName));
    } catch (final Exception e) {
      this.errors.receive(this.errorInvalidTypeName(lexical, typeName));
      return Optional.empty();
    }
  }

  /**
   * Bind an attribute by checking the validity of the name, and resolving the type reference.
   */

  private Optional<MADeclAttribute<MSchemaBoundType>> bindAttribute(
    final MetaSchemaIdentifier currentSchema,
    final List<MADeclImport<MSchemaBoundType>> imports,
    final List<MADeclType<MSchemaBoundType>> typeDecls,
    final MADeclAttribute<Parsed> element)
  {
    LOG.trace("binding attribute declaration: {}", element.name());

    final var attributeNameOpt =
      this.bindAttributeName(element.lexical(), element.name());
    final var typeReferenceOpt =
      this.bindTypeReference(currentSchema, imports, typeDecls, element.type());

    return attributeNameOpt.flatMap(attributeName -> {
      return typeReferenceOpt.flatMap(typeReference -> {
        final var commentOpt = element.comment().flatMap(MSCVBinder::bindComment);

        return Optional.of(
          MADeclAttribute.<MSchemaBoundType>builder()
            .setCardinality(element.cardinality())
            .setComment(commentOpt)
            .setData(new Bound(attributeName))
            .setLexical(element.lexical())
            .setName(attributeName.name())
            .setType(typeReference)
            .build());
      });
    });
  }

  private Optional<AttributeName> bindAttributeName(
    final LexicalPosition<URI> lexical,
    final String name)
  {
    try {
      return Optional.of(AttributeName.of(name));
    } catch (final Exception e) {
      this.errors.receive(this.errorInvalidAttributeName(lexical, name));
      return Optional.empty();
    }
  }

  private Optional<MATypeReferenceNamed<MSchemaBoundType>> bindTypeReferenceNamed(
    final MetaSchemaIdentifier currentSchema,
    final List<MADeclImport<MSchemaBoundType>> imports,
    final List<MADeclType<MSchemaBoundType>> typeDecls,
    final MATypeReferenceNamed<Parsed> element)
  {
    final var typeSchemaName = element.schema();
    final var typeName = element.name();
    final var lexical = element.lexical();
    final var typeNameOpt = this.bindTypeNameQualified(lexical, typeSchemaName, typeName);

    if (typeSchemaName.equals(currentSchema.name().name())) {
      final var typeOpt = findType(typeDecls, typeName);
      if (typeOpt.isEmpty()) {
        this.errors.receive(this.errorTypeNonexistent(lexical, typeSchemaName, typeName));
        return Optional.empty();
      }

      return typeNameOpt.flatMap(refTypeName -> {
        return Optional.of(
          MATypeReferenceNamed.<MSchemaBoundType>builder()
            .setData(new Bound(refTypeName))
            .setLexical(lexical)
            .setSchema(typeSchemaName)
            .setName(typeName)
            .build());
      });
    }

    final var loadedSchemaOpt =
      findImportFor(imports, typeSchemaName)
        .flatMap(importDecl -> this.loadSchemaFromImport(currentSchema, importDecl));

    if (loadedSchemaOpt.isEmpty()) {
      this.errors.receive(this.errorTypeNonexistent(lexical, typeSchemaName, typeName));
      return Optional.empty();
    }

    return loadedSchemaOpt.flatMap(loadedSchema -> {
      return typeNameOpt.flatMap(refTypeName -> {
        final var typeNameNQ = refTypeName.name();
        final var typesByName = loadedSchema.typesByName();
        if (typesByName.containsKey(typeNameNQ)) {
          return Optional.of(
            MATypeReferenceNamed.<MSchemaBoundType>builder()
              .setData(new Bound(refTypeName))
              .setLexical(lexical)
              .setSchema(typeSchemaName)
              .setName(typeNameNQ.name())
              .build());
        }

        this.errors.receive(this.errorTypeNonexistent(lexical, typeSchemaName, typeName));
        return Optional.empty();
      });
    });
  }

  private Optional<MetaSchema> loadSchemaFromImport(
    final MetaSchemaIdentifier currentSchema,
    final MADeclImport<MSchemaBoundType> importDecl)
  {
    return this.loadSchema(currentSchema, importDecl.data().name(MetaSchemaIdentifier.class));
  }

  private Optional<MetaSchema> loadSchema(
    final MetaSchemaIdentifier currentSchema,
    final MetaSchemaIdentifier identifier)
  {
    final var schemaOpt = this.loader.load(currentSchema, identifier);
    schemaOpt.ifPresent(loaded -> {
      final var loadedId = loaded.identifier();
      Invariants.checkInvariantV(
        loadedId,
        Objects.equals(loadedId, identifier),
        "Loaded schema identifier %s must match expected identifier %s",
        loadedId.show(),
        identifier.show());
    });
    return schemaOpt;
  }

  private Optional<MATypeReferenceType<MSchemaBoundType>> bindTypeReference(
    final MetaSchemaIdentifier currentSchema,
    final List<MADeclImport<MSchemaBoundType>> imports,
    final List<MADeclType<MSchemaBoundType>> typeDecls,
    final MATypeReferenceType<Parsed> element)
  {
    LOG.trace("binding type reference: {}", element.referenceKind());

    switch (element.referenceKind()) {
      case REFERENCE_PRIMITIVE:
        return bindTypeReferencePrimitive(
          (MATypeReferencePrimitive<Parsed>) element)
          .map(Function.identity());
      case REFERENCE_NAMED:
        return this.bindTypeReferenceNamed(
          currentSchema, imports, typeDecls, (MATypeReferenceNamed<Parsed>) element)
          .map(Function.identity());
    }
    throw new UnreachableCodeException();
  }

  private Optional<TypeNameQualified> bindTypeNameQualified(
    final LexicalPosition<URI> lexical,
    final String schemaName,
    final String typeName)
  {

    return this.bindSchemaName(lexical, schemaName).flatMap(schemaNameAct -> {
      return this.bindTypeName(lexical, typeName).flatMap(typeNameAct -> {
        return Optional.of(TypeNameQualified.of(schemaNameAct, typeNameAct));
      });
    });
  }

  private Optional<MetaSchemaName> bindSchemaName(
    final LexicalPosition<URI> lexical,
    final String schemaName)
  {
    try {
      return Optional.of(MetaSchemaName.of(schemaName));
    } catch (final Exception e) {
      this.errors.receive(this.errorSchemaNameInvalid(lexical, schemaName, e));
      return Optional.empty();
    }
  }

  private MSchemaCompilerError errorTypeNonexistent(
    final LexicalPosition<URI> lexical,
    final String schemaName,
    final String typeName)
  {
    return MSchemaCompilerError.builder()
      .setAttributes(PresentableAttributes.of(
        PresentableAttributes.entry(this.messages.format("schema"), schemaName),
        PresentableAttributes.entry(this.messages.format("type"), typeName)
      ))
      .setErrorCode(TYPE_NONEXISTENT)
      .setLexical(lexical)
      .setMessage(this.messages.format("errorTypeNonexistent"))
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .build();
  }

  private MSchemaCompilerError errorInvalidAttributeName(
    final LexicalPosition<URI> lexical,
    final String name)
  {
    return MSchemaCompilerError.builder()
      .setAttributes(PresentableAttributes.one(this.messages.format("attribute"), name))
      .setErrorCode(ATTRIBUTE_NAME_INVALID)
      .setLexical(lexical)
      .setMessage(this.messages.format("errorAttributeNameInvalid"))
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .build();
  }

  private MSchemaCompilerError errorInvalidTypeName(
    final LexicalPosition<URI> lexical,
    final String typeName)
  {
    return MSchemaCompilerError.builder()
      .setAttributes(PresentableAttributes.one(this.messages.format("type"), typeName))
      .setErrorCode(TYPE_NAME_INVALID)
      .setLexical(lexical)
      .setMessage(this.messages.format("errorTypeNameInvalid"))
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .build();
  }

  private MSchemaCompilerError errorSchemaNameInvalid(
    final LexicalPosition<URI> lexical,
    final String schemaName,
    final Exception e)
  {
    return MSchemaCompilerError.builder()
      .setAttributes(PresentableAttributes.one(this.messages.format("schema"), schemaName))
      .setErrorCode(SCHEMA_NAME_INVALID)
      .setException(e)
      .setLexical(lexical)
      .setMessage(e.getLocalizedMessage())
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .build();
  }

  private MSchemaCompilerError errorNonexistentSchema(
    final LexicalPosition<URI> lexical,
    final String schemaName)
  {
    return MSchemaCompilerError.builder()
      .setAttributes(PresentableAttributes.one(this.messages.format("schema"), schemaName))
      .setErrorCode(SCHEMA_NONEXISTENT)
      .setLexical(lexical)
      .setMessage(this.messages.format("errorSchemaNonexistent"))
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .build();
  }

  private static final class Bound implements MSchemaBoundType
  {
    private final NameType name;

    Bound(
      final NameType inName)
    {
      this.name = Objects.requireNonNull(inName, "name");
    }

    @Override
    public <T extends NameType> T name(final Class<T> nameType)
    {
      if (Objects.equals(this.name.getClass(), nameType)) {
        return nameType.cast(this.name);
      }

      throw new IllegalArgumentException(
        String.format("This name is of type %s", this.name.getClass().getCanonicalName()));
    }
  }

  private static final class BoundNothing implements MSchemaBoundType
  {
    BoundNothing()
    {

    }

    @Override
    public <T extends NameType> T name(final Class<T> nameType)
    {
      throw new IllegalArgumentException("This element has no name");
    }
  }
}
