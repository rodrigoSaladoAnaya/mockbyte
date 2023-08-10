package com.mockbyte.html;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HTTPInputStreamMock extends InputStream {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final HTTPMetaInfo meta;
  private final HTTPRecorder recorder;
  private FileInputStream input;

  public HTTPInputStreamMock(HTTPMetaInfo meta, HTTPRecorder recorder) {
    this.meta = meta;
    this.recorder = recorder;
  }

  public void mock() throws IOException {
    log.info("MOCK <- {}", recorder.getFile());
    var file = recorder.getFile();
    if (!file.exists()) {
      log.error("First run RECORD");
      return;
    }
    input = new FileInputStream(file);
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
