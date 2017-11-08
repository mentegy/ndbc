package io.trane.ndbc.mysql.encoding;

import io.trane.ndbc.proto.BufferReader;
import io.trane.ndbc.proto.BufferWriter;
import io.trane.ndbc.value.ShortValue;

final class ShortEncoding extends Encoding<Short, ShortValue> {

  @Override
  public final Integer oid() {
    return Oid.INT2;
  }

  @Override
  public final Class<ShortValue> valueClass() {
    return ShortValue.class;
  }

  @Override
  public final String encodeText(final Short value) {
    return Short.toString(value);
  }

  @Override
  public final Short decodeText(final String value) {
    return Short.valueOf(value);
  }

  @Override
  public final void encodeBinary(final Short value, final BufferWriter b) {
    b.writeShort(value);
  }

  @Override
  public final Short decodeBinary(final BufferReader b) {
    return b.readShort();
  }

  @Override
  protected ShortValue box(Short value) {
    return new ShortValue(value);
  }

  @Override
  protected Short unbox(ShortValue value) {
    return value.getShort();
  }
}
