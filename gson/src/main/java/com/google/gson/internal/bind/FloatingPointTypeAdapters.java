/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.internal.bind;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

final class FloatingPointTypeAdapters {
  static final TypeAdapter<Number> FLOAT = new FloatAdapter(false);
  static final TypeAdapter<Number> FLOAT_STRICT = new FloatAdapter(true);

  static final TypeAdapter<Number> DOUBLE = new DoubleAdapter(false);
  static final TypeAdapter<Number> DOUBLE_STRICT = new DoubleAdapter(true);

  private FloatingPointTypeAdapters() {
    throw new UnsupportedOperationException();
  }

  private static final class FloatAdapter extends TypeAdapter<Number> {
    private final boolean strict;

    private FloatAdapter(boolean strict) {
      this.strict = strict;
    }

    @Override
    public Float read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return (float) in.nextDouble();
    }

    @Override
    public void write(JsonWriter out, Number value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      float floatValue = value.floatValue();
      if (strict) {
        checkValidFloatingPoint(floatValue);
      }
      // For backward compatibility don't call `JsonWriter.value(float)` because that method has
      // been newly added and not all custom JsonWriter implementations might override it yet
      Number floatNumber = value instanceof Float ? value : floatValue;
      out.value(floatNumber);
    }
  }

  private static final class DoubleAdapter extends TypeAdapter<Number> {
    private final boolean strict;

    private DoubleAdapter(boolean strict) {
      this.strict = strict;
    }

    @Override
    public Double read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return in.nextDouble();
    }

    @Override
    public void write(JsonWriter out, Number value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      double doubleValue = value.doubleValue();
      if (strict) {
        checkValidFloatingPoint(doubleValue);
      }
      out.value(doubleValue);
    }
  }

  private static void checkValidFloatingPoint(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException(
          value
              + " is not a valid double value as per JSON specification. To override this"
              + " behavior, use GsonBuilder.serializeSpecialFloatingPointValues() method.");
    }
  }
}
