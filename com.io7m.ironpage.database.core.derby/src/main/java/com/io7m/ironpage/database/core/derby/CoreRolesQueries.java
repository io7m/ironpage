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

package com.io7m.ironpage.database.core.derby;

import com.io7m.ironpage.database.core.api.CDException;
import com.io7m.ironpage.database.core.api.CDRolesQueriesType;
import com.io7m.ironpage.database.core.api.CDSecurityRoleDTO;
import com.io7m.ironpage.presentable.api.PresentableAttributes;
import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.io7m.ironpage.errors.api.ErrorSeverity.SEVERITY_ERROR;

final class CoreRolesQueries implements CDRolesQueriesType
{
  private final DSLContext dslContext;

  CoreRolesQueries(
    final Clock inClock,
    final Connection inConnection)
  {
    final var connection = Objects.requireNonNull(inConnection, "connection");
    final var settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    this.dslContext = DSL.using(connection, SQLDialect.DERBY, settings);
  }

  private static CDException handleUpdateException(
    final String name,
    final Exception e)
  {
    final var cause = e.getCause();
    if (cause instanceof DerbySQLIntegrityConstraintViolationException) {
      switch (((DerbySQLIntegrityConstraintViolationException) cause).getConstraintName()) {
        case "ROLE_NAME_UNIQUE": {
          return new CDException(
            SEVERITY_ERROR,
            ROLE_ALREADY_EXISTS,
            CoreMessages.localize("errorRoleAlreadyExists", name),
            e,
            PresentableAttributes.of(PresentableAttributes.entry(CoreMessages.localize("role"), name)));
        }
        default: {
          break;
        }
      }
    }

    return genericDatabaseException(e);
  }

  private static CDSecurityRoleDTO roleFromRecord(
    final Record3<Long, String, String> record)
  {
    return CDSecurityRoleDTO.builder()
      .setId(record.getValue(CoreTables.FIELD_ROLE_ID).longValue())
      .setDescription(record.getValue(CoreTables.FIELD_ROLE_DESCRIPTION))
      .setName(record.getValue(CoreTables.FIELD_ROLE_NAME))
      .build();
  }

  private static CDException genericDatabaseException(
    final Exception e)
  {
    return new CDException(
      SEVERITY_ERROR,
      DATABASE_ERROR,
      CoreMessages.localize("errorDatabase", e.getLocalizedMessage()),
      e);
  }

  @Override
  public CDSecurityRoleDTO roleCreate(
    final String name,
    final String description)
    throws CDException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");

    try (var query =
           this.dslContext.insertInto(CoreTables.TABLE_ROLES)
             .set(CoreTables.FIELD_ROLE_NAME, name)
             .set(CoreTables.FIELD_ROLE_DESCRIPTION, description)) {
      query.execute();
    } catch (final DataAccessException e) {
      throw handleUpdateException(name, e);
    }

    return this.roleGetForName(name).get();
  }

  @Override
  public Optional<CDSecurityRoleDTO> roleGet(
    final long id)
    throws CDException
  {
    try (var query =
           this.dslContext.select(
             CoreTables.FIELD_ROLE_ID,
             CoreTables.FIELD_ROLE_NAME,
             CoreTables.FIELD_ROLE_DESCRIPTION)
             .from(CoreTables.TABLE_ROLES)
             .where(CoreTables.FIELD_ROLE_ID.eq(Long.valueOf(id)))) {
      final var rows = query.fetch();
      if (rows.size() != 1) {
        return Optional.empty();
      }
      return Optional.of(roleFromRecord(rows.get(0)));
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  @Override
  public Optional<CDSecurityRoleDTO> roleGetForName(
    final String name)
    throws CDException
  {
    Objects.requireNonNull(name, "name");

    try (var query =
           this.dslContext.select(
             CoreTables.FIELD_ROLE_ID,
             CoreTables.FIELD_ROLE_NAME,
             CoreTables.FIELD_ROLE_DESCRIPTION)
             .from(CoreTables.TABLE_ROLES)
             .where(CoreTables.FIELD_ROLE_NAME.eq(name))) {
      return Optional.of(roleFromRecord(query.fetchOne()));
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  @Override
  public CDSecurityRoleDTO roleUpdate(
    final CDSecurityRoleDTO role)
    throws CDException
  {
    Objects.requireNonNull(role, "role");

    final var idBox = Long.valueOf(role.id());
    try (var query =
           this.dslContext.update(CoreTables.TABLE_ROLES)
             .set(CoreTables.FIELD_ROLE_NAME, role.name())
             .set(CoreTables.FIELD_ROLE_DESCRIPTION, role.description())
             .where(CoreTables.FIELD_ROLE_ID.eq(idBox))) {
      final var results = query.execute();
      if (results != 1) {
        throw new CDException(
          SEVERITY_ERROR,
          ROLE_NONEXISTENT,
          CoreMessages.localize("errorRoleNonexistent", idBox),
          null,
          PresentableAttributes.of(PresentableAttributes.entry(CoreMessages.localize("roleID"), idBox.toString()))
        );
      }
      return role;
    } catch (final DataAccessException e) {
      throw handleUpdateException(role.name(), e);
    }
  }

  @Override
  public Stream<CDSecurityRoleDTO> roleList()
    throws CDException
  {
    try (var query =
           this.dslContext.select(
             CoreTables.FIELD_ROLE_ID,
             CoreTables.FIELD_ROLE_NAME,
             CoreTables.FIELD_ROLE_DESCRIPTION)
             .from(CoreTables.TABLE_ROLES)
             .orderBy(CoreTables.FIELD_ROLE_ID.asc())) {
      return query.fetchStream()
        .map(CoreRolesQueries::roleFromRecord);
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }
}
