package com.mockbyte.http;

import com.mockbyte.Args;
import com.mockbyte.config.ConfigHttp;
import com.mockbyte.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class RecordFlow extends ProxyFlow {

  private static final Logger log = LoggerFactory.getLogger(RecordFlow.class);

  public static Runnable create(Args args, ConfigHttp config, Socket localSocket) {
    return () -> {
      var instance = new RecordFlow();
      var tx = Tx.create(config);
      try (
        var remoteSocket = Server.getRemoteSocket(args, config);
        var inputStream = RecordStream.create(args, config, tx, localSocket.getInputStream(), remoteSocket.getOutputStream());
        var outputStream = RecordStream.create(args, config, tx, remoteSocket.getInputStream(), localSocket.getOutputStream());
      ) {
        instance.http(tx, inputStream, outputStream);
      } catch (IOException | NoSuchAlgorithmException cause) {
        throw new RuntimeException(cause);
      }
    };
  }

}
