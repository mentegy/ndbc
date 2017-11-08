package io.trane.ndbc.mysql.netty4;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.trane.future.Future;
import io.trane.ndbc.Config;
import io.trane.ndbc.Config.SSL.Mode;

public class InitSSLHandler {

  public Future<Void> apply(final String host, final int port, final Optional<Config.SSL> optCfg,
      final NettyChannel channel) {
    return optCfg.map(cfg -> {
      final SslContextBuilder ctxBuilder = SslContextBuilder.forClient();
      if (cfg.mode() == Mode.VERIFY_CA || cfg.mode() == Mode.VERIFY_FULL)
        cfg.rootCert().map(ctxBuilder::trustManager).orElseGet(() -> {
          try {
            final TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            final FileInputStream cacerts = new FileInputStream(
                System.getProperty("java.home") + "/lib/security/cacerts");
            try {
              ks.load(cacerts, "changeit".toCharArray());
            } finally {
              cacerts.close();
            }
            tmf.init(ks);
            return ctxBuilder.trustManager(tmf);
          } catch (final Exception e) {
            throw new RuntimeException(e);
          }
        });
      else
        ctxBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);

      SslContext sslContext;
      try {
        sslContext = ctxBuilder.build();
      } catch (final SSLException e) {
        throw new RuntimeException(e);
      }

      return channel.ctx().onSuccess(ctx -> {
        final SSLEngine sslEngine = sslContext.newEngine(ctx.alloc(), host, port);
        if (cfg.mode() == Mode.VERIFY_FULL) {
          final SSLParameters sslParams = sslEngine.getSSLParameters();
          sslParams.setEndpointIdentificationAlgorithm("HTTPS");
          sslEngine.setSSLParameters(sslParams);
        }
        final SslHandler handler = new SslHandler(sslEngine);
        ctx.pipeline().addFirst(handler);
      }).voided();
    }).orElse(Future.VOID);
  }

}
