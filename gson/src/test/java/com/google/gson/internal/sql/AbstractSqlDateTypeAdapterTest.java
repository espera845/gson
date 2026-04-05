/*
 * Copyright (C) 2024 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.sql.Time;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the shared logic in {@link AbstractSqlDateTypeAdapter}, exercised via its two concrete
 * subclasses {@link SqlDateTypeAdapter} and {@link SqlTimeTypeAdapter}.
 */
// Suppression for `java.sql.Date` to make it explicit that this is not `java.util.Date`
@SuppressWarnings("UnnecessarilyFullyQualified")
public class AbstractSqlDateTypeAdapterTest {

  private Gson gson;
  private TimeZone oldTimeZone;
  private Locale oldLocale;

  @Before
  public void setUp() {
    this.oldTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    this.oldLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
    gson = new Gson();
  }

  @After
  public void tearDown() {
    TimeZone.setDefault(oldTimeZone);
    Locale.setDefault(oldLocale);
  }

  /** Tests that null is correctly serialized to JSON null for java.sql.Date. */
  @Test
  public void testSqlDateNullSerialization() {
    String json = gson.toJson(null, java.sql.Date.class);
    assertThat(json).isEqualTo("null");
  }

  /** Tests that null is correctly serialized to JSON null for java.sql.Time. */
  @Test
  public void testSqlTimeNullSerialization() {
    String json = gson.toJson(null, Time.class);
    assertThat(json).isEqualTo("null");
  }

  /** Tests that JSON null is correctly deserialized to null for java.sql.Date. */
  @Test
  public void testSqlDateNullDeserialization() {
    java.sql.Date result = gson.fromJson("null", java.sql.Date.class);
    assertThat(result).isNull();
  }

  /** Tests that JSON null is correctly deserialized to null for java.sql.Time. */
  @Test
  public void testSqlTimeNullDeserialization() {
    Time result = gson.fromJson("null", Time.class);
    assertThat(result).isNull();
  }

  /**
   * Tests that a malformed date string throws a JsonSyntaxException whose message includes the type
   * name "SQL Date" — exercising the {@code getTypeName()} method of the abstract base class.
   */
  @Test
  public void testSqlDateInvalidInputThrowsWithTypeName() {
    JsonSyntaxException e =
        assertThrows(
            JsonSyntaxException.class, () -> gson.fromJson("\"not-a-date\"", java.sql.Date.class));
    assertThat(e).hasMessageThat().contains("SQL Date");
    assertThat(e).hasMessageThat().contains("not-a-date");
  }

  /**
   * Tests that a malformed time string throws a JsonSyntaxException whose message includes the type
   * name "SQL Time" — exercising the {@code getTypeName()} method of the abstract base class.
   */
  @Test
  public void testSqlTimeInvalidInputThrowsWithTypeName() {
    JsonSyntaxException e =
        assertThrows(JsonSyntaxException.class, () -> gson.fromJson("\"not-a-time\"", Time.class));
    assertThat(e).hasMessageThat().contains("SQL Time");
    assertThat(e).hasMessageThat().contains("not-a-time");
  }
}
