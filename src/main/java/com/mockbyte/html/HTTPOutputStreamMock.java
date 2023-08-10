package com.mockbyte.html;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class HTTPOutputStreamMock extends OutputStream {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  public HTTPOutputStreamMock() {
  }


  @Override
  public void write(byte[] b, int off, int len) throws IOException {
  }

  @Override
  public void write(byte[] b) throws IOException {

  }

  @Override
  public void write(int b) throws IOException {
  }

}