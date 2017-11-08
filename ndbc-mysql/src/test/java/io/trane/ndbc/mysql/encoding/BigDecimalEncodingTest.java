package io.trane.ndbc.mysql.encoding;

import java.math.BigDecimal;

import io.trane.ndbc.value.BigDecimalValue;

public class BigDecimalEncodingTest extends EncodingTest<BigDecimalValue, BigDecimalEncoding> {

  public BigDecimalEncodingTest() {
    super(
        new BigDecimalEncoding(),
        Oid.NUMERIC,
        BigDecimalValue.class,
        r -> new BigDecimalValue(BigDecimal.valueOf(r.nextLong(), r.nextInt(100))));
  }

}
