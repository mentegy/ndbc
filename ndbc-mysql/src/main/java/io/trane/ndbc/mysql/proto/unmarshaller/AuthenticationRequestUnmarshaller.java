package io.trane.ndbc.mysql.proto.unmarshaller;

import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest;
import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest.AuthenticationCleartextPassword;
import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest.AuthenticationGSS;
import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest.AuthenticationGSSContinue;
import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest.AuthenticationKerberosV5;
import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest.AuthenticationMD5Password;
import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest.AuthenticationOk;
import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest.AuthenticationSCMCredential;
import io.trane.ndbc.mysql.proto.Message.AuthenticationRequest.AuthenticationSSPI;
import io.trane.ndbc.proto.BufferReader;

final class AuthenticationRequestUnmarshaller {

  private final AuthenticationOk                authenticationOk                = new AuthenticationOk();
  private final AuthenticationKerberosV5        authenticationKerberosV5        = new AuthenticationKerberosV5();
  private final AuthenticationCleartextPassword authenticationCleartextPassword = new AuthenticationCleartextPassword();
  private final AuthenticationSCMCredential     authenticationSCMCredential     = new AuthenticationSCMCredential();
  private final AuthenticationGSS               authenticationGSS               = new AuthenticationGSS();
  private final AuthenticationSSPI              authenticationSSPI              = new AuthenticationSSPI();

  public final AuthenticationRequest decode(final BufferReader b) {
    switch (b.readInt()) {

      case 0:
        return authenticationOk;
      case 2:
        return authenticationKerberosV5;
      case 3:
        return authenticationCleartextPassword;
      case 5:
        return new AuthenticationMD5Password(b.readBytes(4));
      case 6:
        return authenticationSCMCredential;
      case 7:
        return authenticationGSS;
      case 9:
        return authenticationSSPI;
      case 8:
        new AuthenticationGSSContinue(b.readBytes());
      default:
        throw new IllegalStateException("Invalid authentication request type");
    }
  }

}
