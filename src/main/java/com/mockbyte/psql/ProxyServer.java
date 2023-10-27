package com.mockbyte.psql;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer implements Closeable {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final Config config;
  @Getter
  private ServerSocket serverSocket;

  private ProxyServer(Config config) {
    this.config = config;
  }

  private void createServerSocket() throws IOException {
    serverSocket = new ServerSocket(4646);
  }

  public Socket getNewConnection() throws IOException {
    var socket = serverSocket.accept();
    return socket;
  }

  public static ProxyServer create(Config config) throws IOException {
    var instance = new ProxyServer(config);
    instance.createServerSocket();
    return instance;
  }

  @Override
  public void close() throws IOException {
    if (serverSocket != null) {
      serverSocket.close();
    }
  }

}
