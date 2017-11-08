package io.trane.ndbc.mysql.encoding;

import io.trane.ndbc.value.LocalTimeValue;

public class LocalTimeEncodingTest
    extends EncodingTest<LocalTimeValue, LocalTimeEncoding> {

  public LocalTimeEncodingTest() {
    super(
        new LocalTimeEncoding(),
        Oid.TIME,
        LocalTimeValue.class,
        r -> new LocalTimeValue(randomLocalDateTime(r).toLocalTime()));
  }
}
