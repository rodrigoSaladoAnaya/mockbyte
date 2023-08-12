package com.mockbyte.http;

import com.mockbyte.Args;
import com.mockbyte.config.ConfigHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class MockFlow {

  private static final Logger log = LoggerFactory.getLogger(MockFlow.class);

  public static Runnable create(Args args, ConfigHttp config, Socket localSocket) {
    return () -> {
      var tx = Tx.create(config);
      try (
        var inputMock = new MockInputStream(config, tx);
        var outputMock = new MockOutputStream();
        var inputStream = MockStream.create(config, tx, localSocket.getInputStream(), outputMock);
        var outputStream = MockStream.create(config, tx, inputMock, localSocket.getOutputStream());
      ) {
        tx.reset(Tx.Type.REQ);
        log.info("REQ_INI -> {}", tx);
        inputStream.writeCommand();
        if (tx.isChunked()) {
          inputStream.writeChunked();
        } else {
          inputStream.writeFixedLength();
        }
        inputStream.endRequest();
        log.info("REQ_END -> {}", tx);

        do {
          tx.reset(Tx.Type.RES);
          log.info("RES_INI -> {}", tx);
          outputStream.writeCommand();
          if (tx.isChunked()) {
            outputStream.writeChunked();
          } else {
            outputStream.writeFixedLength();
          }
          outputStream.endRequest();
          log.info("RES_END -> {}", tx);
        } while (tx.getContentLength() == -1);/**/
      } catch (IOException | NoSuchAlgorithmException cause) {
        throw new RuntimeException(cause);
      }
    };
  }

}
