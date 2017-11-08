package io.trane.ndbc.mysql.encoding;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import io.trane.ndbc.proto.BufferReader;
import io.trane.ndbc.proto.BufferWriter;
import io.trane.ndbc.value.OffsetTimeValue;

final class OffsetTimeEncoding extends Encoding<OffsetTime, OffsetTimeValue> {

  private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(DateTimeFormatter.ISO_LOCAL_TIME).optionalStart().appendOffset("+HH:MM:ss", "Z")
      .optionalEnd()
      .optionalStart().appendOffset("+HH:mm", "Z").optionalEnd().optionalStart()
      .appendOffset("+HH", "Z").optionalEnd()
      .toFormatter();

  @Override
  public final Integer oid() {
    return Oid.TIMETZ;
  }

  @Override
  public final Class<OffsetTimeValue> valueClass() {
    return OffsetTimeValue.class;
  }

  @Override
  public final String encodeText(final OffsetTime value) {
    return value.toString();
  }

  @Override
  public final OffsetTime decodeText(final String value) {
    return OffsetTime.parse(value, formatter);
  }

  @Override
  public final void encodeBinary(final OffsetTime value, final BufferWriter b) {
    b.writeLong(value.toLocalTime().toNanoOfDay() / 1000);
    b.writeInt(-value.getOffset().getTotalSeconds());
  }

  @Override
  public final OffsetTime decodeBinary(final BufferReader b) {
    final LocalTime time = LocalTime.ofNanoOfDay(b.readLong() * 1000);
    final ZoneOffset zone = ZoneOffset.ofTotalSeconds(-b.readInt());
    return time.atOffset(zone);
  }

  @Override
  protected OffsetTimeValue box(OffsetTime value) {
    return new OffsetTimeValue(value);
  }

  @Override
  protected OffsetTime unbox(OffsetTimeValue value) {
    return value.getOffsetTime();
  }
}
