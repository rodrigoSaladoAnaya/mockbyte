package com.mockbyte.psql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FrontendServer implements Closeable {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final Config config;
  private ServerSocket serverSocket;

  private FrontendServer(Config config) {
    this.config = config;
  }

  private void createServerSocket() throws IOException {
    serverSocket = new ServerSocket(4646);
  }

  public Socket getNewConnection() throws IOException {
    return serverSocket.accept();
  }

  public static FrontendServer create(Config config) throws IOException {
    var instance = new FrontendServer(config);
    instance.createServerSocket();
    return instance;
  }

  @Override
  public void close() throws IOException {
    if (serverSocket != null) {
      serverSocket.close();
    }
  }

  public boolean isClosed() {
    if (serverSocket != null) {
      return serverSocket.isClosed();
    }
    return false;
  }

}
