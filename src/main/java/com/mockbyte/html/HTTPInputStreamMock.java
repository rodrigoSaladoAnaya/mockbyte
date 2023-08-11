package com.mockbyte.html;

import com.mockbyte.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class HTTPInputStreamMock extends InputStream {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final Config config;
  private final HTTPRecorder recorder;
  private FileInputStream input;

  public HTTPInputStreamMock(Config config, HTTPRecorder recorder) {
    this.config = config;
    this.recorder = recorder;
  }

  public void mock() throws IOException, InterruptedException {
    var file = recorder.getFile();
    if (!file.exists()) {
      log.error("First run RECORD");
      return;
    }
    input = new FileInputStream(file);
    var mockMetaFile = new File(String.format("%s_%s", file.getPath(), "meta"));
    if (mockMetaFile.exists()) {
      log.info("MOCK_FILE -> [{}]", mockMetaFile);
      HTTPMBKMeta metaConfig = Config.objectMapper.readValue(mockMetaFile, HTTPMBKMeta.class);
      log.info("Meta Config -> {}", config);
      log.info("MOCK_DELAY -> {}", metaConfig.getElapsed());
      TimeUnit.MILLISECONDS.sleep(metaConfig.getElapsed());
    }
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return input.read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b) throws IOException {
    return input.read(b);
  }

  @Override
  public int read() throws IOException {
    return input.read();
  }

  @Override
  public void close() throws IOException {
    input.close();
  }

}
