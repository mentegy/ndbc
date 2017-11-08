package io.trane.ndbc.mysql.proto;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest;
import io.trane.ndbc.mysql.proto.Message.BackendKeyData;
import io.trane.ndbc.mysql.proto.Message.ParameterStatus;
import io.trane.ndbc.mysql.proto.Message.PasswordMessage;
import io.trane.ndbc.mysql.proto.Message.ReadyForQuery;
import io.trane.ndbc.mysql.proto.Message.StartupMessage;
import io.trane.ndbc.mysql.util.MD5Digest;
import io.trane.ndbc.proto.Exchange;
import io.trane.ndbc.proto.ServerMessage;
import io.trane.ndbc.util.PartialFunction;

public final class StartupExchange {

  public final Exchange<Optional<BackendKeyData>> apply(final Charset charset, final String user,
      final Optional<String> password, final Optional<String> database) {
    return Exchange.send(startupMessage(charset, user, database))
        .thenReceive(
            authenticationOk
                .orElse(clearTextPasswordAuthentication(password))
                .orElse(md5PasswordAuthentication(charset, user, password))
                .orElse(unsupportedAuthentication))
        .then(waitForBackendStartup(Optional.empty())).onFailure(ex -> Exchange.CLOSE);
  }

  private final PartialFunction<ServerMessage, Exchange<Void>> authenticationOk = PartialFunction
      .when(AuthenticationRequest.AuthenticationOk.class, msg -> Exchange.VOID);

  private final PartialFunction<ServerMessage, Exchange<Void>> clearTextPasswordAuthentication(
      final Optional<String> password) {
    return PartialFunction.when(AuthenticationRequest.AuthenticationCleartextPassword.class,
        msg -> withRequiredPassword(password,
            p -> Exchange.send(new PasswordMessage(p)).thenReceive(authenticationOk)));
  }

  private final PartialFunction<ServerMessage, Exchange<Void>> md5PasswordAuthentication(
      final Charset charset,
      final String user, final Optional<String> password) {
    return PartialFunction.when(AuthenticationRequest.AuthenticationMD5Password.class,
        msg -> withRequiredPassword(password,
            p -> Exchange.send(md5PasswordMessage(charset, user, p, msg.salt))
                .thenReceive(authenticationOk)));
  }

  private final PartialFunction<ServerMessage, Exchange<Void>> unsupportedAuthentication = PartialFunction
      .when(
          AuthenticationRequest.class,
          msg -> Exchange.CLOSE
              .thenFail("Database authentication method not supported by ndbc: " + msg));

  private final Exchange<Optional<BackendKeyData>> waitForBackendStartup(
      final Optional<BackendKeyData> backendKeyData) {
    return Exchange
        .receive(PartialFunction.<ServerMessage, Exchange<Optional<BackendKeyData>>>apply()
            .orElse(BackendKeyData.class, msg -> waitForBackendStartup(Optional.of(msg)))
            .orElse(ParameterStatus.class, msg -> waitForBackendStartup(backendKeyData))
            .orElse(ReadyForQuery.class, msg -> Exchange.value(backendKeyData)));
  }

  private final Exchange<Void> withRequiredPassword(final Optional<String> password,
      final Function<String, Exchange<Void>> f) {
    return password.map(f)
        .orElse(Exchange
            .fail("Database requires a password but the configuration doesn't specify one."));
  }

  private final PasswordMessage md5PasswordMessage(final Charset charset, final String user,
      final String password,
      final byte[] salt) {
    final byte[] bytes = MD5Digest.encode(user.getBytes(charset), password.getBytes(charset), salt);
    return new PasswordMessage(new String(bytes, charset));
  }

  private final StartupMessage startupMessage(final Charset charset, final String user,
      final Optional<String> database) {
    final List<StartupMessage.Parameter> params = new ArrayList<>();
    database.ifPresent(db -> params.add(new StartupMessage.Parameter("database", db)));
    params.add(new StartupMessage.Parameter("client_encoding", charset.name()));
    params.add(new StartupMessage.Parameter("DateStyle", "ISO"));
    params.add(new StartupMessage.Parameter("extra_float_digits", "2"));
    return new StartupMessage(user, params.toArray(new StartupMessage.Parameter[0]));
  }
}
