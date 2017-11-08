package io.trane.ndbc.mysql.proto.unmarshaller;

import java.util.Optional;
import java.util.logging.Logger;

import io.trane.ndbc.mysql.proto.Message;
import io.trane.ndbc.mysql.proto.Message.BackendKeyData;
import io.trane.ndbc.mysql.proto.Message.BindComplete;
import io.trane.ndbc.mysql.proto.Message.CloseComplete;
import io.trane.ndbc.mysql.proto.Message.CopyBothResponse;
import io.trane.ndbc.mysql.proto.Message.CopyData;
import io.trane.ndbc.mysql.proto.Message.CopyDone;
import io.trane.ndbc.mysql.proto.Message.CopyInResponse;
import io.trane.ndbc.mysql.proto.Message.CopyOutResponse;
import io.trane.ndbc.mysql.proto.Message.EmptyQueryResponse;
import io.trane.ndbc.mysql.proto.Message.FunctionCallResponse;
import io.trane.ndbc.mysql.proto.Message.InfoResponse;
import io.trane.ndbc.mysql.proto.Message.NoData;
import io.trane.ndbc.mysql.proto.Message.NotificationResponse;
import io.trane.ndbc.mysql.proto.Message.ParameterDescription;
import io.trane.ndbc.mysql.proto.Message.ParameterStatus;
import io.trane.ndbc.mysql.proto.Message.ParseComplete;
import io.trane.ndbc.mysql.proto.Message.PortalSuspended;
import io.trane.ndbc.mysql.proto.Message.ReadyForQuery;
import io.trane.ndbc.mysql.proto.Message.SSLResponse;
import io.trane.ndbc.proto.BufferReader;

public final class Unmarshaller {

  private static final Logger                     log                = Logger
      .getLogger(Unmarshaller.class.getName());

  private final AuthenticationRequestUnmarshaller authenticationRequestUnmarshaller;
  private final CommandCompleteUnmarshaller       commandCompleteUnmarshaller;
  private final DataRowUnmarshaller               dataRowUnmarshaller;
  private final InfoResponseFieldsUnmarshaller    infoResponseFieldsUnmarshaller;
  private final RowDescriptionUnmarshaller        rowDescriptionUnmarshaller;

  private final BindComplete                      bindComplete       = new BindComplete();
  private final CloseComplete                     closeComplete      = new CloseComplete();
  private final CopyDone                          copyDone           = new CopyDone();
  private final EmptyQueryResponse                emptyQueryResponse = new EmptyQueryResponse();
  private final NoData                            noData             = new NoData();
  private final ParseComplete                     parseComplete      = new ParseComplete();
  private final PortalSuspended                   portalSuspended    = new PortalSuspended();

  public Unmarshaller() {
    super();
    authenticationRequestUnmarshaller = new AuthenticationRequestUnmarshaller();
    commandCompleteUnmarshaller = new CommandCompleteUnmarshaller();
    dataRowUnmarshaller = new DataRowUnmarshaller();
    infoResponseFieldsUnmarshaller = new InfoResponseFieldsUnmarshaller();
    rowDescriptionUnmarshaller = new RowDescriptionUnmarshaller();
  }

  public final Optional<Message> decode(final boolean ssl, final BufferReader b) throws Exception {
    try {
      if (b.readableBytes() == 1 && ssl)
        return Optional.of(decode(b.readByte(), b.readSlice(0)));
      else if (b.readableBytes() >= 5) {
        b.markReaderIndex();
        final byte tpe = b.readByte();
        final int length = b.readInt() - 4;
        if (b.readableBytes() >= length)
          return Optional.of(decode(tpe, b.readSlice(length)));
        else {
          b.resetReaderIndex();
          return Optional.empty();
        }
      } else
        return Optional.empty();
    } catch (final Exception e) {
      log.severe("Can't parse msg " + e);
      throw e;
    }
  }

  private final Message decode(final byte tpe, final BufferReader b) {
    switch (tpe) {
      case 'R':
        return authenticationRequestUnmarshaller.decode(b);
      case 'K':
        return new BackendKeyData(b.readInt(), b.readInt());
      case '2':
        return bindComplete;
      case '3':
        return closeComplete;
      case 'C':
        return commandCompleteUnmarshaller.decode(b);
      case 'd':
        return new CopyData(b.readBytes());
      case 'c':
        return copyDone;
      case 'D':
        return dataRowUnmarshaller.decode(b);
      case 'I':
        return emptyQueryResponse;
      case 'E':
        return new InfoResponse.ErrorResponse(infoResponseFieldsUnmarshaller.decode(b));
      case 'n':
        return noData;
      case 'N':
        if (b.readableBytes() == 0)
          return new SSLResponse(false);
        else
          return new InfoResponse.NoticeResponse(infoResponseFieldsUnmarshaller.decode(b));
      case 'A':
        return new NotificationResponse(b.readInt(), b.readCString(), b.readCString());
      case 't':
        return new ParameterDescription(b.readInts(b.readShort()));
      case 'S':
        if (b.readableBytes() == 0)
          return new SSLResponse(true);
        else
          return new ParameterStatus(b.readCString(), b.readCString());
      case '1':
        return parseComplete;
      case 's':
        return portalSuspended;
      case 'Z':
        return new ReadyForQuery(b.readByte());
      case 'T':
        return rowDescriptionUnmarshaller.decode(b);
      case 'G':
        return notImplemented(CopyInResponse.class);
      case 'H':
        return notImplemented(CopyOutResponse.class);
      case 'W':
        return notImplemented(CopyBothResponse.class);
      case 'V':
        return notImplemented(FunctionCallResponse.class);
      default:
        throw new IllegalStateException("Invalid server message type: " + (char) tpe);
    }
  }

  private final Message notImplemented(final Class<?> cls) {
    throw new UnsupportedOperationException("Unmarshaller not implemented for class: " + cls);
  }
}
