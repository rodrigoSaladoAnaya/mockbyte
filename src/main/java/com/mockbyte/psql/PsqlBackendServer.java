package com.mockbyte.psql;

import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PsqlBackendServer implements Closeable {

  private final Config config;
  private Socket socket;

  private PsqlBackendServer(Config config) {
    this.config = config;
  }

  private void createSocket() throws IOException {
    socket = new Socket("localhost", 5432);
  }

  public InputStream getInputStream() throws IOException {
    return socket.getInputStream();
  }

  public OutputStream getOutputStream() throws IOException {
    return socket.getOutputStream();
  }

  public static PsqlBackendServer create(Config config) throws IOException {
    var instance = new PsqlBackendServer(config);
    instance.createSocket();
    return instance;
  }

  @Override
  public void close() throws IOException {
    if (socket != null) {
      socket.close();
    }
  }

}
