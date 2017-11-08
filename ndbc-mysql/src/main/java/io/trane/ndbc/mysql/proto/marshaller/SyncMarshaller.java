package io.trane.ndbc.mysql.proto.marshaller;

import io.trane.ndbc.mysql.proto.Message.Sync;
import io.trane.ndbc.proto.BufferWriter;

public final class SyncMarshaller {

  public final void encode(final Sync msg, final BufferWriter b) {
    b.writeChar('S');
    b.writeInt(4);
  }
}
