package com.mockbyte.http;

import com.mockbyte.Args;
import com.mockbyte.config.ConfigHttp;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class MockFlow implements Flow {

  public static Runnable create(Args args, ConfigHttp config, Socket localSocket) {
    return () -> {
      var instance = new MockFlow();
      var tx = Tx.create(config);
      try (
        var inputMock = new MockInputStream(config, tx);
        var outputMock = new MockOutputStream();
        var inputStream = MockStream.create(tx, localSocket.getInputStream(), outputMock);
        var outputStream = MockStream.create(tx, inputMock, localSocket.getOutputStream());
      ) {
        instance.http(tx, inputStream, outputStream);
      } catch (IOException | NoSuchAlgorithmException cause) {
        throw new RuntimeException(cause);
      }
    };
  }

}
