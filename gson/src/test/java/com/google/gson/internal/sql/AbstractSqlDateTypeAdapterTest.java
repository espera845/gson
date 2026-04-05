/*
 * Copyright (C) 2026 Google Inc.
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
import static org.junit.Assert.fail;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import org.junit.Test;

@SuppressWarnings("JavaUtilDate")
public class AbstractSqlDateTypeAdapterTest {
  private static final class TestSqlDate extends Date {
    private static final long serialVersionUID = 1L;

    TestSqlDate(long time) {
      super(time);
    }
  }

  private static final class TestAdapter extends AbstractSqlDateTypeAdapter<TestSqlDate> {
    private TestAdapter() {
      super("MMM d, yyyy", "Test SQL Date");
    }

    @Override
    protected TestSqlDate createSqlDate(Date date) {
      return new TestSqlDate(date.getTime());
    }
  }

  private final TestAdapter adapter = new TestAdapter();

  @Test
  public void readNullReturnsNull() throws IOException {
    JsonReader reader = new JsonReader(new StringReader("null"));

    assertThat(adapter.read(reader)).isNull();
  }

  @Test
  public void writeNullWritesJsonNull() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);

    adapter.write(writer, null);

    assertThat(stringWriter.toString()).isEqualTo("null");
  }

  @Test
  public void readInvalidValueIncludesTypeAndPath() throws IOException {
    JsonReader reader = new JsonReader(new StringReader("{\"value\":\"not-a-date\"}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("value");

    try {
      adapter.read(reader);
      fail("Expected JsonSyntaxException");
    } catch (JsonSyntaxException e) {
      assertThat(e)
          .hasMessageThat()
          .contains("Failed parsing 'not-a-date' as Test SQL Date; at path $.value");
    }
  }

  @Test
  public void readAndWriteUseConfiguredPattern() throws IOException {
    TestSqlDate value = new TestSqlDate(1259836800000L);

    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    adapter.write(writer, value);

    assertThat(stringWriter.toString()).isEqualTo("\"Dec 3, 2009\"");

    TestSqlDate parsed = adapter.read(new JsonReader(new StringReader("\"Dec 3, 2009\"")));
    assertThat(parsed.getTime()).isEqualTo(value.getTime());
  }
}
