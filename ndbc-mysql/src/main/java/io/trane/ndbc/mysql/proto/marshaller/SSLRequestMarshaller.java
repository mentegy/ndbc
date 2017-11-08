package io.trane.ndbc.mysql.proto.marshaller;

import io.trane.ndbc.mysql.proto.Message.SSLRequest;
import io.trane.ndbc.proto.BufferWriter;

public final class SSLRequestMarshaller {

  public final void encode(final SSLRequest msg, final BufferWriter b) {
    b.writeInt(8);
    b.writeInt(80877103);
  }
}
