package com.mockbyte;

import com.mockbyte.config.Config;
import com.mockbyte.proxy.HTTPProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

  public static final Logger log = LoggerFactory.getLogger(Server.class);

  public static void main(String[] args) throws IOException {
    Server.create(args);
  }

  private Server(Config config, Command command) throws IOException {
    start(config, command);
  }

  private void start(Config config, Command command) throws IOException {
    try (var serverSocket = new ServerSocket(config.getLocalPort())) {
      log.info("Server listen listen on port [{}]", serverSocket.getLocalPort());

      while (!serverSocket.isClosed()) {
        var localSocket = serverSocket.accept();
        runProxy(config, command, localSocket);
      }
    }
  }

  private void runProxy(Config config, Command command, Socket localSocket) {
    Thread.ofVirtual()
      .name("mockbyte-server-", 0)
      .start(() -> {
        log.info("Client connected on port [{}]", localSocket.getLocalPort());
        try (
          var remoteSocket = getRemoteSocket(config, command)
        ) {
          switch (config.getType()) {
            case HTTP -> HTTPProxy.create(config, command, localSocket, remoteSocket);
            case POSTGRESQL -> {
            }
            case ISO8583 -> {
            }
            case GRPC -> {
            }
            default -> throw new IllegalStateException("Unexpected value: " + config.getType());
          }
        } catch (IOException e) {
          log.error("Error with the remote server [{}:{}]", config.getRemoteHost(), config.getRemotePort(), e);
        }
      });
  }

  private Socket getRemoteSocket(Config config, Command command) throws IOException {
    if (command == Command.MOCK) {
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

  private static void create(String[] args) throws IOException {
    var config = Config.fromPath(args[0]);
    var command = Command.valueOf(args[1]);
    new Server(config, command);
  }

}
