package io.trane.ndbc.mysql.encoding;

import io.trane.ndbc.value.LongValue;

public class LongEncodingTest extends EncodingTest<LongValue, LongEncoding> {

  public LongEncodingTest() {
    super(
        new LongEncoding(),
        Oid.INT8,
        LongValue.class,
        r -> new LongValue(r.nextLong()));
  }

}
