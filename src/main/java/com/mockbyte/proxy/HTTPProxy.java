package com.mockbyte.proxy;

import com.mockbyte.Command;
import com.mockbyte.Config;
import com.mockbyte.html.HTTPMetaInfo;
import com.mockbyte.html.HTTPRecorder;
import com.mockbyte.html.HTTPStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

public final class HTTPProxy implements Proxy {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final Command command;
  private final HTTPMetaInfo meta;
  private final Socket localSocket;
  private final Socket remoteSocket;
  private final HTTPRecorder recorder;

  private HTTPProxy(HTTPMetaInfo meta, Command command, Socket localSocket, Socket remoteSocket, HTTPRecorder recorder) {
    this.command = command;
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
      meta.clearAsType(HTTPMetaInfo.Type.REQ);
      meta.incrementTxs();
      localStream.writeCommand();
      log.info("REQ {}", meta);
      if (meta.isChunked()) {
        localStream.writeChunked();
      } else {
        localStream.writeFixedLength();
      }
      recorder.close();

      meta.clearAsType(HTTPMetaInfo.Type.RES);
      while (meta.getContentLength() == -1) {
        meta.incrementTxs();
        remoteStream.writeCommand();
        if (meta.isChunked()) {
          remoteStream.writeChunked();
        } else {
          remoteStream.writeFixedLength();
        }
        log.info("RES {}", meta);
        recorder.close();
      }
    } catch (InterruptedException ex) {
      log.error("Error during proxy", ex);
    }
  }

  private void mock() throws IOException {
  }

  public static void create(Config config, Command command, Socket localSocket, Socket remoteSocket) throws IOException {
    var meta = HTTPMetaInfo.create(config);
    var recorder = HTTPRecorder.create(meta);
    var instance = new HTTPProxy(meta, command, localSocket, remoteSocket, recorder);
    switch (command) {
      case PROXY, RECORD -> instance.proxy();
      case MOCK -> instance.mock();
    }

  }

}
