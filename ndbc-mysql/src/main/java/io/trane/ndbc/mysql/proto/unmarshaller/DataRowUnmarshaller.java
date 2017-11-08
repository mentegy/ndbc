package io.trane.ndbc.mysql.proto.unmarshaller;

import io.trane.ndbc.mysql.proto.Message.DataRow;
import io.trane.ndbc.proto.BufferReader;;

final class DataRowUnmarshaller {

  public final DataRow decode(final BufferReader b) {
    final short columns = b.readShort();
    final BufferReader[] values = new BufferReader[columns];
    for (short i = 0; i < columns; i++) {
      final int length = b.readInt();
      if (length == -1)
        values[i] = null;
      else {
        final BufferReader slice = b.readSlice(length);
        slice.retain();
        values[i] = slice;
      }
    }
    return new DataRow(values);
  }
}
