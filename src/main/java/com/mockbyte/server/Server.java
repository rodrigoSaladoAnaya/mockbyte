package com.mockbyte.server;

import com.mockbyte.Args;
import com.mockbyte.config.ConfigHttp;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;

public sealed interface Server permits ServerHttp {

  void start() throws IOException;

  static Socket getRemoteSocket(Args args, ConfigHttp config) throws IOException {
    if (args.getCommand() == Args.Command.MOCK) {
      return null;
    }
    if (config.isSsl()) {
      var socketFactory = SSLSocketFactory.getDefault();
      var socket = (SSLSocket) socketFactory.createSocket(config.getRemoteHost(), config.getRemotePort());
      socket.startHandshake();
      return socket;
    } else {
      var socket = new Socket(config.getRemoteHost(), config.getRemotePort());
      return socket;
    }
  }
}
