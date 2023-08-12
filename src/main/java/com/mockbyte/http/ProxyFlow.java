package com.mockbyte.http;

import com.mockbyte.Args;
import com.mockbyte.config.ConfigHttp;
import com.mockbyte.server.Server;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class ProxyFlow implements Flow {

  public static Runnable create(Args args, ConfigHttp config, Socket localSocket) {
    return () -> {
      var instance = new ProxyFlow();
      var tx = Tx.create(config);
      try (
        var remoteSocket = Server.getRemoteSocket(args, config);
        var inputStream = ProxyStream.create(tx, localSocket.getInputStream(), remoteSocket.getOutputStream());
        var outputStream = ProxyStream.create(tx, remoteSocket.getInputStream(), localSocket.getOutputStream());
      ) {
        instance.http(tx, inputStream, outputStream);
      } catch (IOException | NoSuchAlgorithmException cause) {
        throw new RuntimeException(cause);
      }
    };
  }


}
