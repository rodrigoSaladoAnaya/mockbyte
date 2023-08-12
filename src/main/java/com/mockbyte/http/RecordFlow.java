package com.mockbyte.http;

import com.mockbyte.Args;
import com.mockbyte.config.ConfigHttp;
import com.mockbyte.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class RecordFlow {

  private static final Logger log = LoggerFactory.getLogger(RecordFlow.class);

  public static Runnable execute(Args args, ConfigHttp config, Socket localSocket) {
    return () -> {
      var tx = Tx.create(config);
      try (
        var remoteSocket = Server.getRemoteSocket(args, config);
        var inputStream = RecordStream.create(args, config, tx, localSocket.getInputStream(), remoteSocket.getOutputStream());
        var outputStream = RecordStream.create(args, config, tx, remoteSocket.getInputStream(), localSocket.getOutputStream());
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
        } while (tx.getContentLength() == -1);
      } catch (IOException | NoSuchAlgorithmException cause) {
        throw new RuntimeException(cause);
      }
    };
  }

}
