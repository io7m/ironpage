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

package com.io7m.ironpage.metadata.schema.compiler.vanilla.typed;

import com.io7m.ironpage.metadata.schema.ast.MACardinality;
import com.io7m.ironpage.metadata.schema.ast.MADeclAttribute;
import com.io7m.ironpage.metadata.schema.ast.MADeclImport;
import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.ast.MADeclType;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferenceNamed;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferencePrimitive;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferenceType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaBoundType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.typed.api.MSchemaCompilerTyperType;
import com.io7m.ironpage.metadata.schema.types.api.AttributeCardinality;
import com.io7m.ironpage.metadata.schema.types.api.AttributeName;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaAttribute;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.TypeName;
import com.io7m.ironpage.metadata.schema.types.api.TypeNameQualified;
import com.io7m.ironpage.metadata.schema.types.api.TypeNamed;
import com.io7m.ironpage.metadata.schema.types.api.TypePrimitive;
import com.io7m.ironpage.metadata.schema.types.api.TypeReferenceNamed;
import com.io7m.ironpage.metadata.schema.types.api.TypeReferencePrimitive;
import com.io7m.ironpage.metadata.schema.types.api.TypeReferenceType;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

final class MSCVTyper implements MSchemaCompilerTyperType
{
  private static final Logger LOG = LoggerFactory.getLogger(MSCVTyper.class);

  private final MADeclSchema<MSchemaBoundType> schema;
  private final MSchemaCompilerLoaderType loader;
  private final MSchemaCompilerMessagesType messages;
  private final MSCVSafeErrorConsumer errors;
  private final URI uri;

  MSCVTyper(
    final MSchemaCompilerErrorConsumerType inErrors,
    final MSchemaCompilerLoaderType inLoader,
    final MSchemaCompilerMessagesType inMessages,
    final URI inUri,
    final MADeclSchema<MSchemaBoundType> inSchema)
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

  private static Optional<MetaSchemaIdentifier> findImportForType(
    final List<MetaSchemaIdentifier> imports,
    final TypeNameQualified typeName)
  {
    return imports.stream()
      .filter(i -> Objects.equals(i.name(), typeName.schema()))
      .findFirst();
  }

  private static List<MetaSchemaIdentifier> checkImports(
    final List<MADeclImport<MSchemaBoundType>> imports)
  {
    return imports.stream()
      .map(MSCVTyper::checkImport)
      .collect(Collectors.toUnmodifiableList());
  }

  private static MetaSchemaIdentifier checkImport(
    final MADeclImport<MSchemaBoundType> importDecl)
  {
    LOG.debug("checking import: {}", importDecl.schemaName());
    return importDecl.data().name(MetaSchemaIdentifier.class);
  }

  private static AttributeCardinality checkAttributeCardinality(
    final MACardinality cardinality)
  {
    switch (cardinality) {
      case CARDINALITY_1:
        return AttributeCardinality.CARDINALITY_1;
      case CARDINALITY_0_TO_1:
        return AttributeCardinality.CARDINALITY_0_TO_1;
      case CARDINALITY_0_TO_N:
        return AttributeCardinality.CARDINALITY_0_TO_N;
      case CARDINALITY_1_TO_N:
        return AttributeCardinality.CARDINALITY_1_TO_N;
    }
    throw new UnreachableCodeException();
  }

  private static Optional<TypeNamed> findType(
    final List<TypeNamed> types,
    final TypeName name)
  {
    return types.stream()
      .filter(type -> Objects.equals(type.name(), name))
      .findFirst();
  }

  private static Optional<TypeReferencePrimitive> checkTypeReferencePrimitive(
    final MATypeReferencePrimitive<MSchemaBoundType> type)
  {
    switch (type.primitive()) {
      case TYPE_BOOLEAN:
        return Optional.of(TypeReferencePrimitive.of(TypePrimitive.TYPE_BOOLEAN));
      case TYPE_INTEGER:
        return Optional.of(TypeReferencePrimitive.of(TypePrimitive.TYPE_INTEGER));
      case TYPE_REAL:
        return Optional.of(TypeReferencePrimitive.of(TypePrimitive.TYPE_REAL));
      case TYPE_STRING:
        return Optional.of(TypeReferencePrimitive.of(TypePrimitive.TYPE_STRING));
      case TYPE_TIMESTAMP:
        return Optional.of(TypeReferencePrimitive.of(TypePrimitive.TYPE_TIMESTAMP));
      case TYPE_URI:
        return Optional.of(TypeReferencePrimitive.of(TypePrimitive.TYPE_URI));
      case TYPE_UUID:
        return Optional.of(TypeReferencePrimitive.of(TypePrimitive.TYPE_UUID));
    }
    throw new UnreachableCodeException();
  }

  @Override
  public Optional<MetaSchema> execute()
  {
    LOG.debug("executing typer for {}", this.uri);

    final var imports =
      checkImports(this.schema.imports());
    final var types =
      this.checkTypes(imports, this.schema.types());
    final var attributes =
      this.checkAttributes(imports, types, this.schema.attributes());

    if (this.errors.isFailed()) {
      return Optional.empty();
    }

    return Optional.of(
      MetaSchema.builder()
        .setAttributes(attributes)
        .setImports(imports)
        .setTypes(types)
        .setIdentifier(this.schema.data().name(MetaSchemaIdentifier.class))
        .build());
  }

  private List<TypeNamed> checkTypes(
    final List<MetaSchemaIdentifier> imports,
    final Map<String, MADeclType<MSchemaBoundType>> types)
  {
    final var results = new ArrayList<TypeNamed>(types.size());
    for (final var type : types.values()) {
      final var result = this.checkType(imports, results, type);
      result.ifPresent(results::add);
    }
    return List.copyOf(results);
  }

  private Optional<TypeNamed> checkType(
    final List<MetaSchemaIdentifier> imports,
    final List<TypeNamed> types,
    final MADeclType<MSchemaBoundType> type)
  {
    LOG.debug("checking type: {}", type.name());

    final var baseType = type.baseType();
    final var typeReferenceOpt = this.checkTypeReference(imports, types, baseType);

    return typeReferenceOpt.flatMap(typeReference -> {
      return Optional.of(
        TypeNamed.builder()
          .setName(type.data().name(TypeName.class))
          .setType(typeReference)
          .build());
    });
  }

  private Optional<TypeReferenceType> checkTypeReference(
    final List<MetaSchemaIdentifier> imports,
    final List<TypeNamed> types,
    final MATypeReferenceType<MSchemaBoundType> reference)
  {
    switch (reference.referenceKind()) {
      case REFERENCE_PRIMITIVE:
        return checkTypeReferencePrimitive(
          (MATypeReferencePrimitive<MSchemaBoundType>) reference)
          .map(Function.identity());
      case REFERENCE_NAMED:
        return this.checkTypeReferenceNamed(
          imports, types, (MATypeReferenceNamed<MSchemaBoundType>) reference)
          .map(Function.identity());
    }

    throw new UnimplementedCodeException();
  }

  private Optional<TypeReferenceNamed> checkTypeReferenceNamed(
    final List<MetaSchemaIdentifier> imports,
    final List<TypeNamed> types,
    final MATypeReferenceNamed<MSchemaBoundType> typeReference)
  {
    final var currentSchemaId =
      this.schema.data().name(MetaSchemaIdentifier.class);
    final var typeName =
      typeReference.data().name(TypeNameQualified.class);

    if (Objects.equals(typeName.schema(), currentSchemaId.name())) {
      final var typeNamed =
        findType(types, typeName.name())
          .orElseThrow(() -> new IllegalStateException(
            String.format("Failed to find type '%s' in current schema", typeName.name().show())));

      return Optional.of(
        TypeReferenceNamed.builder()
          .setName(typeNamed.name())
          .setSchema(currentSchemaId)
          .setBasePrimitiveType(typeNamed.type().basePrimitiveType())
          .build());
    }

    final var importDecl =
      findImportForType(imports, typeName)
        .orElseThrow(() -> new IllegalStateException(
          String.format("Failed to find type '%s' in imports", typeName.show())));

    final var schemaLoaded =
      this.loader.load(currentSchemaId, importDecl)
        .orElseThrow(() -> new IllegalStateException(
          String.format("Failed to load schema '%s'", importDecl.show())));

    final var schemaTypes = schemaLoaded.typesByName();

    final var loadedBaseType = schemaTypes.get(typeName.name());
    if (loadedBaseType == null) {
      throw new IllegalStateException(
        String.format(
          "Unexpectedly failed to find type '%s' in schema '%s'",
          typeName.show(),
          schemaLoaded.identifier().show()));
    }

    return Optional.of(
      TypeReferenceNamed.builder()
        .setName(typeName.name())
        .setSchema(schemaLoaded.identifier())
        .setBasePrimitiveType(loadedBaseType.type().basePrimitiveType())
        .build());
  }

  private List<MetaSchemaAttribute> checkAttributes(
    final List<MetaSchemaIdentifier> imports,
    final List<TypeNamed> types,
    final Map<String, MADeclAttribute<MSchemaBoundType>> attributes)
  {
    return attributes.values()
      .stream()
      .map(attribute -> this.checkAttribute(imports, types, attribute))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toUnmodifiableList());
  }

  private Optional<MetaSchemaAttribute> checkAttribute(
    final List<MetaSchemaIdentifier> imports,
    final List<TypeNamed> types,
    final MADeclAttribute<MSchemaBoundType> attribute)
  {
    LOG.debug("checking attribute: {}", attribute.name());

    final var attributeName = attribute.data().name(AttributeName.class);
    return this.checkTypeReference(imports, types, attribute.type()).flatMap(type -> {
      return Optional.of(
        MetaSchemaAttribute.builder()
          .setCardinality(checkAttributeCardinality(attribute.cardinality()))
          .setName(attributeName)
          .setType(type)
          .build());
    });
  }

  @Override
  public void close()
  {

  }

}
