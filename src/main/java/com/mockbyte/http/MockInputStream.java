package com.mockbyte.http;

import com.mockbyte.config.Config;
import com.mockbyte.config.ConfigHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MockInputStream extends InputStream {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final ConfigHttp config;
  private final Tx tx;
  private FileInputStream input;

  public MockInputStream(ConfigHttp config, Tx tx) {
    this.config = config;
    this.tx = tx;
    tx.getCount().incrementAndGet();
  }

  public void mock() throws IOException {
    tx.getCount().incrementAndGet();
    setMkbFile();
    var meta = getMeta();
    if (meta.getElapsed() != 0) {
      log.info("MOCK_SLEEP -> {}ms", meta.getElapsed());
      Config.sleep(meta.getElapsed());
    }
  }

  private Meta getMeta() throws IOException {
    var file = getMockFile(RecordStream.SUFFIX.meta);
    log.info("MOCK_META_FILE -> uuid: {}, file: {}", tx.getUuid(), file);
    return ConfigHttp.objectMapper.readValue(file, Meta.class);
  }

  private void setMkbFile() throws IOException {
    var file = getMockFile(RecordStream.SUFFIX.mkb);
    input = new FileInputStream(file);
    log.info("MOCK_MKB_FILE -> uuid: {}, file: {}", tx.getUuid(), file);
  }

  private File getMockFile(RecordStream.SUFFIX suffix) throws IOException {
    var file = RecordStream.getFile(config, tx, suffix);
    if (!file.exists()) {
      throw new RuntimeException(String.format("MOCK_FILE_ERROR -> uuid: %s, file: %s", tx.getUuid(), file));
    }
    return file;
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
    if (input != null) {
      input.close();
    }
  }

}
