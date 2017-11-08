package io.trane.ndbc.mysql.netty4;

import java.time.Duration;

import io.trane.ndbc.Config;
import io.trane.ndbc.DataSource;

public class TestEnv {

  private final Config config  = Config
      .apply("io.trane.ndbc.mysql.netty4.DataSourceSupplier", "localhost", 5432, "mysql")
      .password("mysql")
      .poolValidationInterval(Duration.ofSeconds(1))
      .poolMaxSize(1).poolMaxWaiters(0);                                                       // .ssl(SSL.apply(SSL.Mode.REQUIRE));

  protected DataSource ds      = DataSource.fromConfig(config);

  protected Duration   timeout = Duration.ofSeconds(999);

}
