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

package com.io7m.ironpage.security.vanilla;

import com.io7m.ironpage.security.api.SPolicy;
import com.io7m.ironpage.security.api.SPolicyRule;
import com.io7m.ironpage.security.api.SPolicySerializerType;
import com.io7m.ironpage.security.vanilla.v1.SPP1Constants;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

final class SPolicySerializer implements SPolicySerializerType
{
  private final SPolicy policy;
  private final OutputStream output;
  private final ByteArrayOutputStream buffer;

  SPolicySerializer(
    final SPolicy inPolicy,
    final OutputStream inOutput)
  {
    this.policy = Objects.requireNonNull(inPolicy, "policy");
    this.output = Objects.requireNonNull(inOutput, "output");
    this.buffer = new ByteArrayOutputStream(1024);
  }

  private static void writeCommentOptionally(
    final String namespaceURI,
    final XMLStreamWriter writer,
    final String comment)
    throws XMLStreamException
  {
    if (!comment.isEmpty()) {
      writer.writeStartElement(namespaceURI, "Comment");
      writer.writeCharacters(comment);
      writer.writeEndElement();
    }
  }

  private static void writeRole(
    final String namespaceURI,
    final XMLStreamWriter writer,
    final SPolicyRule rule)
    throws XMLStreamException
  {
    writer.writeStartElement(namespaceURI, "Rule");
    writer.writeAttribute("conclusion", rule.conclude().toString());

    final var labelOpt = rule.label();
    if (labelOpt.isPresent()) {
      final var label = labelOpt.get();
      writer.writeAttribute("label", label.label());
    }

    final var permissionOpt = rule.permission();
    if (permissionOpt.isPresent()) {
      final var permission = permissionOpt.get();
      writer.writeAttribute("permission", permission.permission());
    }

    final var roleOpt = rule.role();
    if (roleOpt.isPresent()) {
      final var role = roleOpt.get();
      writer.writeAttribute("role", role.role());
    }

    writeCommentOptionally(namespaceURI, writer, rule.comment());
    writer.writeEndElement();
  }

  @Override
  public void execute()
    throws XMLStreamException, IOException, TransformerException
  {
    final var namespaceURI = SPP1Constants.POLICY_1_0_NAMESPACE.toString();
    final var outputs = XMLOutputFactory.newInstance();

    final var writer = outputs.createXMLStreamWriter(this.buffer, "UTF-8");
    writer.writeStartDocument("UTF-8", "1.0");
    writer.setPrefix("p", namespaceURI);
    writer.writeStartElement(namespaceURI, "SecurityPolicy");
    writer.writeNamespace("p", namespaceURI);
    writer.writeAttribute("version", this.policy.version().toString());

    writeCommentOptionally(namespaceURI, writer, this.policy.comment());

    for (final var rule : this.policy.rules()) {
      writeRole(namespaceURI, writer, rule);
    }

    writer.writeEndElement();
    writer.writeEndDocument();
    this.buffer.flush();

    final var transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(
      new StreamSource(new ByteArrayInputStream(this.buffer.toByteArray())),
      new StreamResult(this.output));

    this.output.flush();
  }

  @Override
  public void close()
    throws IOException
  {
    this.output.close();
  }
}
