package com.mockbyte.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockbyte.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static com.mockbyte.MockByte.objectMapper;

public sealed interface Config permits ConfigHttp {

  static final Logger log = LoggerFactory.getLogger(Config.class);

  static Config create(Args args) throws IOException {
    var file = new File(args.getConfigPath());
    if (!file.exists()) {
      throw new RuntimeException(String.format("Invalid file [%s]", args.getConfigPath()));
    }
    Config config = switch (args.getServerType()) {
      case HTTP -> objectMapper.readValue(file, ConfigHttp.class);
      case POSTGRESQL -> null;
      case ISO8583 -> null;
      case GRPC -> null;
    };
    log.info("Config -> {}", config);
    return config;
  }

}
