/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.internal.sql;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/** Shared parsing and formatting logic for SQL date-like type adapters. */
@SuppressWarnings("JavaUtilDate")
abstract class AbstractSqlDateTypeAdapter<T extends Date> extends TypeAdapter<T> {
  private final DateFormat format;
  private final String typeDescription;

  protected AbstractSqlDateTypeAdapter(String pattern, String typeDescription) {
    this.format = new SimpleDateFormat(pattern);
    this.typeDescription = typeDescription;
  }

  protected abstract T createSqlDate(Date date);

  @Override
  public final T read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }

    String s = in.nextString();
    synchronized (this) {
      TimeZone originalTimeZone = format.getTimeZone();
      try {
        return createSqlDate(format.parse(s));
      } catch (ParseException e) {
        throw new JsonSyntaxException(
            "Failed parsing '" + s + "' as " + typeDescription + "; at path " + in.getPreviousPath(),
            e);
      } finally {
        format.setTimeZone(originalTimeZone);
      }
    }
  }

  @Override
  public final void write(JsonWriter out, T value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }

    String formattedValue;
    synchronized (this) {
      formattedValue = format.format(value);
    }
    out.value(formattedValue);
  }
}
