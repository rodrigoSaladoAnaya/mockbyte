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
import java.util.concurrent.TimeUnit;

public sealed interface Config permits ConfigHttp {

  Logger log = LoggerFactory.getLogger(Config.class);
  ThreadFactory threadFactory = Thread.ofVirtual().name("mockbyte-", 0L).factory();
  ObjectMapper objectMapper = new ObjectMapper();

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

  static String normalize(String input) {
    var string = input.replace(':', '_');
    string = string.replace('.', '_');
    string = string.replace(',', '_');
    string = string.toLowerCase();
    string = string.replaceAll("\\s+", "_");
    var normalized = Normalizer.normalize(string, Normalizer.Form.NFD);
    normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    return normalized;
  }

  static String md5(String input) throws NoSuchAlgorithmException {
    var md5 = MessageDigest.getInstance("MD5");
    var hashInBytes = md5.digest(input.getBytes(StandardCharsets.UTF_8));
    var sb = new StringBuilder();
    for (byte b : hashInBytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  static void sleep(long millis) {
    try {
      TimeUnit.MILLISECONDS.sleep(millis);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

}
