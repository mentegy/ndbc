package io.trane.ndbc.mysql.proto;

import java.util.List;
import java.util.function.BiFunction;

import io.trane.ndbc.mysql.proto.Message.CommandComplete;
import io.trane.ndbc.mysql.proto.Message.NoData;
import io.trane.ndbc.proto.Exchange;
import io.trane.ndbc.proto.ServerMessage;
import io.trane.ndbc.util.PartialFunction;
import io.trane.ndbc.value.Value;

public final class ExtendedExecuteExchange
    implements BiFunction<String, List<Value<?>>, Exchange<Long>> {

  private final ExtendedExchange extendedExchange;

  public ExtendedExecuteExchange(final ExtendedExchange extendedExchange) {
    super();
    this.extendedExchange = extendedExchange;
  }

  @Override
  public final Exchange<Long> apply(final String query, final List<Value<?>> params) {
    return extendedExchange.apply(query, params,
        Exchange.receive(commandComplete.orElse(noDataAndCommandComplete)));
  }

  private final PartialFunction<ServerMessage, Exchange<Long>> commandComplete          = PartialFunction
      .when(CommandComplete.class, msg -> Exchange.value(msg.rows));

  private final PartialFunction<ServerMessage, Exchange<Long>> noDataAndCommandComplete = PartialFunction
      .when(NoData.class, msg -> Exchange.receive(commandComplete));
}
