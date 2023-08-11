package com.mockbyte.proxy;

import com.mockbyte.Command;
import com.mockbyte.config.Config;
import com.mockbyte.html.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

public final class HTTPProxy implements Proxy {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final Config config;
  private final HTTPMetaInfo meta;
  private final Socket localSocket;
  private final Socket remoteSocket;
  private final HTTPRecorder recorder;

  private HTTPProxy(Config config, HTTPMetaInfo meta, Socket localSocket, Socket remoteSocket, HTTPRecorder recorder) {
    this.config = config;
    this.meta = meta;
    this.localSocket = localSocket;
    this.remoteSocket = remoteSocket;
    this.recorder = recorder;
  }

  private void proxy() throws IOException {
    try (
      var localStream = new HTTPStream(meta, localSocket.getInputStream(), remoteSocket.getOutputStream(), recorder);
      var remoteStream = new HTTPStream(meta, remoteSocket.getInputStream(), localSocket.getOutputStream(), recorder);
    ) {
      meta.reset(HTTPMetaInfo.Type.REQ);
      localStream.writeCommand();
      log.info("REQ -> {}", meta);
      if (meta.isChunked()) {
        localStream.writeChunked();
      } else {
        localStream.writeFixedLength();
      }
      localStream.endTx();

      do {
        meta.reset(HTTPMetaInfo.Type.RES);
        remoteStream.writeCommand();
        if (meta.isChunked()) {
          remoteStream.writeChunked();
        } else {
          remoteStream.writeFixedLength();
        }
        remoteStream.endTx();
        log.info("RES -> {}", meta);
      } while (meta.getContentLength() == -1);
    } catch (InterruptedException ex) {
      log.error("Error during proxy", ex);
    }
  }

  private void mock() throws IOException {
    try (
      var remoteOutput = new HTTPOutputStreamMock();
      var remoteInput = new HTTPInputStreamMock(config, recorder);
      var localStream = new HTTPStream(meta, localSocket.getInputStream(), remoteOutput, recorder);
      var remoteStream = new HTTPStream(meta, remoteInput, localSocket.getOutputStream(), recorder);
    ) {
      meta.reset(HTTPMetaInfo.Type.REQ);
      localStream.writeCommand();
      log.info("REQ -> {}", meta);
      if (meta.isChunked()) {
        localStream.writeChunked();
      } else {
        localStream.writeFixedLength();
      }
      localStream.endTx();

      do {
        meta.reset(HTTPMetaInfo.Type.RES);
        remoteInput.mock();
        remoteStream.writeCommand();
        if (meta.isChunked()) {
          remoteStream.writeChunked();
        } else {
          remoteStream.writeFixedLength();
        }
        remoteStream.endTx();
        remoteInput.close();
        log.info("RES -> {}", meta);
      } while (meta.getContentLength() == -1);
    } catch (InterruptedException ex) {
      log.error("Error during proxy", ex);
    }
  }

  public static void create(Config config, Command command, Socket localSocket, Socket remoteSocket) throws IOException {
    var meta = HTTPMetaInfo.create(config);
    var recorder = HTTPRecorder.create(config, meta, command);
    var instance = new HTTPProxy(config, meta, localSocket, remoteSocket, recorder);
    switch (command) {
      case PROXY, RECORD -> instance.proxy();
      case MOCK -> instance.mock();
    }
  }

}
