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

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * DSL definitions for the core table schema.
 */

final class CoreTables
{
  static final Table<Record> TABLE_USERS =
    DSL.table(DSL.name("core", "users"));
  static final Field<String> FIELD_USER_DISPLAY_NAME =
    DSL.field(DSL.name("user_display_name"), SQLDataType.VARCHAR(128));
  static final Field<String> FIELD_USER_EMAIL =
    DSL.field(DSL.name("user_email"), SQLDataType.VARCHAR(128));
  static final Field<UUID> FIELD_USER_ID =
    DSL.field(DSL.name("user_id"), SQLDataType.UUID);
  static final Field<String> FIELD_USER_LOCKED_REASON =
    DSL.field(DSL.name("user_locked_reason"), SQLDataType.VARCHAR(128));
  static final Field<String> FIELD_USER_PASSWORD_HASH =
    DSL.field(DSL.name("user_password_hash"), SQLDataType.VARCHAR(128));
  static final Field<String> FIELD_USER_PASSWORD_PARAMS =
    DSL.field(DSL.name("user_password_params"), SQLDataType.VARCHAR(256));
  static final Field<String> FIELD_USER_PASSWORD_ALGO =
    DSL.field(DSL.name("user_password_algo"), SQLDataType.VARCHAR(64));

  static final Table<Record> TABLE_SESSIONS =
    DSL.table(DSL.name("core", "sessions"));
  static final Field<String> FIELD_SESSION_ID =
    DSL.field(DSL.name("session_id"), SQLDataType.VARCHAR(36));
  static final Field<UUID> FIELD_SESSION_USER_ID =
    DSL.field(DSL.name("session_user_id"), SQLDataType.UUID);
  static final Field<Timestamp> FIELD_SESSION_UPDATED =
    DSL.field(DSL.name("session_updated"), SQLDataType.TIMESTAMP);

  static final Table<Record> TABLE_LABELS =
    DSL.table(DSL.name("core", "security_labels"));
  static final Field<Long> FIELD_LABEL_ID =
    DSL.field(DSL.name("label_id"), SQLDataType.BIGINT);
  static final Field<String> FIELD_LABEL_NAME =
    DSL.field(DSL.name("label_name"), SQLDataType.VARCHAR(128));
  static final Field<String> FIELD_LABEL_DESCRIPTION =
    DSL.field(DSL.name("label_description"), SQLDataType.VARCHAR(128));

  static final Table<Record> TABLE_ROLES =
    DSL.table(DSL.name("core", "security_roles"));
  static final Field<Long> FIELD_ROLE_ID =
    DSL.field(DSL.name("role_id"), SQLDataType.BIGINT);
  static final Field<String> FIELD_ROLE_NAME =
    DSL.field(DSL.name("role_name"), SQLDataType.VARCHAR(128));
  static final Field<String> FIELD_ROLE_DESCRIPTION =
    DSL.field(DSL.name("role_description"), SQLDataType.VARCHAR(128));

  static final Table<Record> TABLE_ROLE_USERS =
    DSL.table(DSL.name("core", "user_roles"));
  static final Field<UUID> FIELD_ROLE_USER_ID =
    DSL.field(DSL.name("role_user_id"), SQLDataType.UUID);
  static final Field<Long> FIELD_ROLE_ROLE_ID =
    DSL.field(DSL.name("role_role_id"), SQLDataType.BIGINT);

  static final Table<Record> TABLE_BLOBS =
    DSL.table(DSL.name("core", "blobs"));
  static final Field<String> FIELD_BLOB_ID =
    DSL.field(DSL.name("blob_id"), SQLDataType.CHAR(64));
  static final Field<byte[]> FIELD_BLOB_DATA =
    DSL.field(DSL.name("blob_data"), SQLDataType.BLOB(8_000_000));
  static final Field<Long> FIELD_BLOB_REDACTION =
    DSL.field(DSL.name("blob_redaction"), SQLDataType.BIGINT);
  static final Field<String> FIELD_BLOB_MEDIA_TYPE =
    DSL.field(DSL.name("blob_media_type"), SQLDataType.VARCHAR(128));
  static final Field<UUID> FIELD_BLOB_OWNER =
    DSL.field(DSL.name("blob_owner"), SQLDataType.UUID);
  static final Field<Long> FIELD_BLOB_SECURITY_LABEL =
    DSL.field(DSL.name("blob_security_label"), SQLDataType.BIGINT);

  static final Table<Record> TABLE_REDACTIONS =
    DSL.table(DSL.name("core", "redactions"));
  static final Field<UUID> FIELD_REDACTION_USER_ID =
    DSL.field(DSL.name("redaction_user"), SQLDataType.UUID);
  static final Field<Timestamp> FIELD_REDACTION_TIME =
    DSL.field(DSL.name("redaction_time"), SQLDataType.TIMESTAMP);
  static final Field<String> FIELD_REDACTION_REASON =
    DSL.field(DSL.name("redaction_reason"), SQLDataType.VARCHAR(128));
  static final Field<Long> FIELD_REDACTION_ID =
    DSL.field(DSL.name("redaction_id"), SQLDataType.BIGINT);

  private CoreTables()
  {

  }
}
