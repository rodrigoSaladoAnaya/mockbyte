package com.mockbyte.http;

import com.mockbyte.Args;
import com.mockbyte.config.ConfigHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class MockFlow extends ProxyFlow{

  private static final Logger log = LoggerFactory.getLogger(MockFlow.class);

  public static Runnable create(Args args, ConfigHttp config, Socket localSocket) {
    return () -> {
      var instance = new MockFlow();
      var tx = Tx.create(config);
      try (
        var inputMock = new MockInputStream(config, tx);
        var outputMock = new MockOutputStream();
        var inputStream = MockStream.create(config, tx, localSocket.getInputStream(), outputMock);
        var outputStream = MockStream.create(config, tx, inputMock, localSocket.getOutputStream());
      ) {
        instance.http(tx, inputStream, outputStream);
      } catch (IOException | NoSuchAlgorithmException cause) {
        throw new RuntimeException(cause);
      }
    };
  }

}
