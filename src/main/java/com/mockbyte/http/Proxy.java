package com.mockbyte.http;

import com.mockbyte.Args;
import com.mockbyte.config.ConfigHttp;
import com.mockbyte.server.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Proxy implements HttpStream {

  private final InputStream input;
  private final OutputStream output;

  public Proxy(InputStream input, OutputStream output) {
    this.input = input;
    this.output = output;
  }

  public static Proxy create(Args args, ConfigHttp config, Socket localSocket) throws IOException {
    var remoteSocket = Server.getRemoteSocket(args, config);
    var instance = new Proxy(localSocket.getInputStream(), remoteSocket.getOutputStream());
    return instance;
  }

  @Override
  public void close() throws IOException {
    var exception = new IOException();
    try {
      input.close();
    } catch (IOException cause) {
      exception.addSuppressed(cause);
    }
    try {
      output.close();
    } catch (IOException cause) {
      exception.addSuppressed(cause);
    }
    if (exception.getSuppressed().length > 0) {
      throw exception;
    }
  }
}
