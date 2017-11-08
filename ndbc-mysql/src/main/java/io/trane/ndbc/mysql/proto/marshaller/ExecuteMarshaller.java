package io.trane.ndbc.mysql.proto.marshaller;

import io.trane.ndbc.mysql.proto.Message.Execute;
import io.trane.ndbc.proto.BufferWriter;

public final class ExecuteMarshaller {

  public final void encode(final Execute msg, final BufferWriter b) {
    b.writeChar('E');
    b.writeInt(0);

    b.writeCString(msg.portalName);
    b.writeInt(msg.maxNumberOfRows);
    b.writeLength(1);
  }
}
