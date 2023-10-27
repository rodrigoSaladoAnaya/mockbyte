package com.mockbyte.psql;

import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class PsqlServer implements Closeable {

  private final Config config;
  @Getter
  private Socket socket;

  private PsqlServer(Config config) {
    this.config = config;
  }

  private void createSocket() throws IOException {
    socket = new Socket("localhost", 5432);
  }

  public static PsqlServer create(Config config) throws IOException {
    var instance = new PsqlServer(config);
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
