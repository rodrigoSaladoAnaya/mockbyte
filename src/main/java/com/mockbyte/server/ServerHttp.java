package com.mockbyte.server;

import com.mockbyte.Args;
import com.mockbyte.MockByte;
import com.mockbyte.config.Config;
import com.mockbyte.config.ConfigHttp;
import com.mockbyte.http.Mock;
import com.mockbyte.http.Proxy;
import com.mockbyte.http.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class ServerHttp implements Server {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final Args args;
  private final ConfigHttp config;

  private ServerHttp(Args args, ConfigHttp config) {
    this.args = args;
    this.config = config;
  }

  @Override
  public void start() throws IOException {
    try (var serverSocket = new ServerSocket(config.getLocalPort())) {
      log.info("HTTP Server listen on port [{}]", serverSocket.getLocalPort());
      while (!serverSocket.isClosed()) {
        var localSocket = serverSocket.accept();
        log.info("Client connected on port [{}]", localSocket.getPort());
        MockByte.threadFactory.newThread(() -> {
          try (var stream = getStream(args, config, localSocket)) {
          } catch (IOException cause) {
            throw new RuntimeException(cause);
          }
        });
      }
    }
  }

  private Proxy getStream(Args args, ConfigHttp config, Socket localSocket) throws IOException {
    return switch (args.getCommand()) {
      case PROXY -> Proxy.create(args, config, localSocket);
      case RECORD -> Record.create(args, config, localSocket);
      case MOCK -> Mock.create(args, config, localSocket);
    };
  }

  public static Server create(Args args, Config config) {
    var instance = new ServerHttp(args, (ConfigHttp) config);
    return instance;
  }

}
