package com.mockbyte.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface Flow {

  Logger log = LoggerFactory.getLogger(RecordFlow.class);

  default void http(Tx tx, HttpStream inputStream, HttpStream outputStream) throws IOException, NoSuchAlgorithmException {
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
  }

}
