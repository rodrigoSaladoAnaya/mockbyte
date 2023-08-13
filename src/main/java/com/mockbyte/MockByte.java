package com.mockbyte;

import com.mockbyte.config.Config;
import com.mockbyte.server.ServerHttp;

import java.io.IOException;

public class MockByte {

  public static void main(String[] args) throws IOException {
    MockByte.createDefault(args);
  }

  private MockByte(Args args, Config config) throws IOException {
    var server = switch (args.getServerType()) {
      case HTTP -> ServerHttp.create(args, config);
      case POSTGRESQL -> null;
      case ISO8583 -> null;
      case GRPC -> null;
    };
    server.start();
  }

  private static MockByte createDefault(String[] arr) throws IOException {
    var args = Args.create(arr);
    var config = Config.create(args);
    var instance = new MockByte(args, config);
    return instance;
  }

}
