package io.trane.ndbc.mysql.encoding;

import io.trane.ndbc.value.BooleanValue;

public class BooleanEncodingTest extends EncodingTest<BooleanValue, BooleanEncoding> {

  public BooleanEncodingTest() {
    super(
        new BooleanEncoding(),
        Oid.BOOL,
        BooleanValue.class,
        r -> new BooleanValue(r.nextBoolean()));
  }

}
