package io.trane.ndbc.mysql.proto.marshaller;

import io.trane.ndbc.mysql.encoding.EncodingRegistry;
import io.trane.ndbc.mysql.proto.Message.Parse;
import io.trane.ndbc.proto.BufferWriter;
import io.trane.ndbc.value.Value;

public final class ParseMarshaller {

  private final EncodingRegistry encoding;

  public ParseMarshaller(final EncodingRegistry encoding) {
    this.encoding = encoding;
  }
  
  public final void encode(final Parse msg, final BufferWriter b) {
    b.writeChar('P');
    b.writeInt(0);

    b.writeCString(msg.destinationName);
    b.writeCString(msg.query);
    b.writeShort((short) msg.params.size());

    for (final Value<?> v : msg.params)
      b.writeInt(encoding.oid(v));

    b.writeLength(1);
  }
}
