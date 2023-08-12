package com.mockbyte.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockbyte.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.concurrent.ThreadFactory;

public sealed interface Config permits ConfigHttp {

  static final Logger log = LoggerFactory.getLogger(Config.class);
  public static final ThreadFactory threadFactory = Thread.ofVirtual().name("mockbyte", 0L).factory();
  public static final ObjectMapper objectMapper = new ObjectMapper();

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

  public static String normalize(String input) {
    var string = input.replace(':', '_');
    string = string.replace('.', '_');
    string = string.replace(',', '_');
    string = string.toLowerCase();
    string = string.replaceAll("\\s+", "_");
    var normalized = Normalizer.normalize(string, Normalizer.Form.NFD);
    normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    return normalized;
  }

  public static String md5(String input) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] hashInBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : hashInBytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();

  }


}
