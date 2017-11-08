package io.trane.ndbc.mysql.netty4;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.trane.future.Future;
import io.trane.ndbc.Config;
import io.trane.ndbc.DataSource;
import io.trane.ndbc.datasource.Connection;
import io.trane.ndbc.datasource.LockFreePool;
import io.trane.ndbc.datasource.Pool;
import io.trane.ndbc.datasource.PooledDataSource;
import io.trane.ndbc.mysql.encoding.Encoding;
import io.trane.ndbc.mysql.encoding.EncodingRegistry;
import io.trane.ndbc.mysql.proto.ExtendedExchange;
import io.trane.ndbc.mysql.proto.ExtendedExecuteExchange;
import io.trane.ndbc.mysql.proto.ExtendedQueryExchange;
import io.trane.ndbc.mysql.proto.InitSSLExchange;
import io.trane.ndbc.mysql.proto.QueryResultExchange;
import io.trane.ndbc.mysql.proto.SimpleExecuteExchange;
import io.trane.ndbc.mysql.proto.SimpleQueryExchange;
import io.trane.ndbc.mysql.proto.StartupExchange;
import io.trane.ndbc.mysql.proto.marshaller.BindMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.CancelRequestMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.CloseMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.DescribeMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.ExecuteMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.FlushMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.Marshaller;
import io.trane.ndbc.mysql.proto.marshaller.ParseMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.PasswordMessageMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.QueryMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.SSLRequestMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.StartupMessageMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.SyncMarshaller;
import io.trane.ndbc.mysql.proto.marshaller.TerminateMarshaller;
import io.trane.ndbc.mysql.proto.unmarshaller.Unmarshaller;

public final class DataSourceSupplier implements Supplier<DataSource> {

  private final Config                         config;
  private final Supplier<Future<NettyChannel>> channelSupplier;
  private final StartupExchange                startup         = new StartupExchange();
  private final EncodingRegistry               encoding;
  private final InitSSLExchange                initSSLExchange = new InitSSLExchange();
  private final InitSSLHandler                 initSSLHandler  = new InitSSLHandler();

  public DataSourceSupplier(final Config config) {
    this.config = config;
    encoding = new EncodingRegistry(
        config.encodingClasses()
            .map(l -> l.stream().map(this::loadEncoding).collect(Collectors.toList())));
    channelSupplier = new ChannelSupplier(config.charset(), createMarshaller(), new Unmarshaller(),
        new NioEventLoopGroup(config.nioThreads().orElse(0),
            new DefaultThreadFactory("ndbc-netty4", true)),
        config.host(), config.port());
  }

  private final Encoding<?, ?> loadEncoding(final String cls) {
    try {
      return (Encoding<?, ?>) Class.forName(cls).newInstance();
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      throw new RuntimeException(
          "Can't load encoding " + cls + ". Make sure to provide an empty constructor.", e);
    }
  }

  private final Marshaller createMarshaller() {
    return new Marshaller(new BindMarshaller(encoding), new CancelRequestMarshaller(),
        new CloseMarshaller(),
        new DescribeMarshaller(), new ExecuteMarshaller(), new FlushMarshaller(),
        new ParseMarshaller(encoding),
        new QueryMarshaller(), new PasswordMessageMarshaller(), new StartupMessageMarshaller(),
        new SyncMarshaller(),
        new TerminateMarshaller(), new SSLRequestMarshaller());
  }

  private final Supplier<Future<Connection>> createConnection() {
    final QueryResultExchange queryResultExchange = new QueryResultExchange(encoding);
    return () -> {
      final ExtendedExchange extendedExchange = new ExtendedExchange();
      return channelSupplier.get()
          .flatMap(
              channel -> initSSLExchange.apply(config.ssl()).run(channel)
                  .flatMap(ssl -> initSSLHandler.apply(config.host(), config.port(), ssl, channel))
                  .flatMap(v -> startup
                      .apply(config.charset(), config.user(), config.password(), config.database())
                      .run(channel)
                      .map(backendKeyData -> new io.trane.ndbc.mysql.Connection(channel,
                          channelSupplier, backendKeyData,
                          new SimpleQueryExchange(queryResultExchange),
                          new SimpleExecuteExchange(),
                          new ExtendedQueryExchange(queryResultExchange, extendedExchange),
                          new ExtendedExecuteExchange(extendedExchange)))));
    };
  }

  private final Pool<Connection> createPool() {
    return LockFreePool.apply(createConnection(), config.poolMaxSize(), config.poolMaxWaiters(),
        config.poolValidationInterval(),
        new ScheduledThreadPoolExecutor(1, new DefaultThreadFactory("ndbc-pool-scheduler", true)));
  }

  @Override
  public final DataSource get() {
    return new PooledDataSource(createPool());
  }
}
