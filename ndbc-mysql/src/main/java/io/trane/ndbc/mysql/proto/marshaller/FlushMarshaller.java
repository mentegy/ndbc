package io.trane.ndbc.mysql.proto.marshaller;

import io.trane.ndbc.mysql.proto.Message.Flush;
import io.trane.ndbc.proto.BufferWriter;

public final class FlushMarshaller {

  public final void encode(final Flush msg, final BufferWriter b) {
    b.writeChar('H');
    b.writeInt(4);
  }
}
