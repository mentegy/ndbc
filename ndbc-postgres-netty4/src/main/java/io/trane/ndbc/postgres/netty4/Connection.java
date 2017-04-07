package io.trane.ndbc.postgres.netty4;

import java.util.Optional;
import java.util.function.Supplier;

import io.trane.future.Future;
import io.trane.ndbc.PreparedStatement;
import io.trane.ndbc.ResultSet;
import io.trane.ndbc.postgres.proto.Execute;
import io.trane.ndbc.postgres.proto.ExtendedQuery;
import io.trane.ndbc.postgres.proto.Message.BackendKeyData;
import io.trane.ndbc.proto.Channel;

public class Connection implements io.trane.ndbc.Connection {

  private final Channel channel;
  private final Optional<BackendKeyData> backendKeyData;
  private final ExtendedQuery extendedQuery = new ExtendedQuery();
  private final Execute execute = new Execute();

  public Connection(Channel channel, Optional<BackendKeyData> backendKeyData) {
    this.channel = channel;
    this.backendKeyData = backendKeyData;
  }

  @Override
  public Future<ResultSet> query(String query) {
    return null;
  }

  @Override
  public Future<Integer> execute(String command) {
    return execute.apply(command).run(channel);
  }

  @Override
  public Future<ResultSet> query(PreparedStatement query) {
    return null;
  }

  @Override
  public Future<Integer> execute(PreparedStatement query) {
    return null;
  }

  @Override
  public <R> Future<R> withTransaction(Supplier<Future<R>> sup) {
    return null;
  }

  @Override
  public Future<Boolean> isValid() {
    return query("SELECT 1").map(r -> true).rescue(e -> Future.FALSE);
  }

  @Override
  public Future<Void> close() {
    return null;
  }
}