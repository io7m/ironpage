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

package com.io7m.ironpage.tests;

import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorErrorReceiverType;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorMessagesType;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorRequest;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorType;
import com.io7m.ironpage.metadata.attribute.validator.vanilla.MetaValidatorMessages;
import com.io7m.ironpage.metadata.attribute.validator.vanilla.MetaValidators;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.spi.MSchemaCompilerSourceType;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.MSCVCompilers;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.MSCVMessagesProvider;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.loader.MSCVLoaders;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;

import java.net.URI;
import java.util.Locale;

public final class MetaValidatorsTest extends MetaValidatorContract
{
  private final MSCVCompilers compilers = new MSCVCompilers();
  private final MetaValidators validators = new MetaValidators();
  private final MSchemaCompilerMessagesType compilerMessages =
    new MSCVMessagesProvider().createStrings(Locale.getDefault());
  private final MetaValidatorMessagesType validatorMessages =
    new MetaValidatorMessages(Locale.getDefault());

  @Override
  protected MSchemaCompilerLoaderType loader(
    final MSchemaCompilerSourceType source,
    final MSchemaCompilerErrorConsumerType errors)
  {
    return new MSCVLoaders().createLoader(this.compilers, errors, this.compilerMessages, source);
  }

  @Override
  protected MetaSchema schema(
    final MSchemaCompilerSourceType source,
    final String file)
    throws Exception
  {
    try (var compiler =
           this.compilers.createCompiler(
             this.showCompilerError(),
             this.compilerMessages,
             this.loader(source, this.showCompilerError()),
             URI.create(file),
             resource(file))) {
      return compiler.execute().get();
    }
  }

  @Override
  protected MetaValidatorType validator(
    final MetaValidatorErrorReceiverType validationErrors,
    final MetaValidatorRequest request)
  {
    return this.validators.createValidator(validationErrors, this.validatorMessages, request);
  }
}
