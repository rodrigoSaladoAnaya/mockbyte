package com.mockbyte.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class Config {

  public static final Logger log = LoggerFactory.getLogger(Config.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public enum Type {HTTP, POSTGRESQL, ISO8583, GRPC}

  private Type type;
  private String localHost;
  private int localPort;
  private String remoteHost;
  private int remotePort;
  private boolean ssl;
  private String mockDir = "./mkb";
  private List<MockFile> mockFiles = new ArrayList<>();

  public static Config fromPath(String path) {
    try {
      File file = new File(path);
      if (!file.exists()) {
        throw new RuntimeException(String.format("Invalid file [%s]", path));
      }
      Config config = objectMapper.readValue(file, Config.class);
      log.info("Config -> {}", config);
      return config;
    } catch (IOException e) {
      log.error("Error reading configuration file", e);
      throw new RuntimeException(e);
    }
  }

}
