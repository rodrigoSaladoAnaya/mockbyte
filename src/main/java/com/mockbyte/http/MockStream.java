package com.mockbyte.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

public class MockStream extends ProxyStream {

  private final InputStream input;

  public MockStream(Tx tx, InputStream input, OutputStream output) {
    super(tx, input, output);
    this.input = input;
  }

  public void writeCommand() throws IOException, NoSuchAlgorithmException {
    if (input instanceof MockInputStream ins) {
      ins.mock();
    }
    super.writeCommand();
  }

  public void writeHttpRequest(byte[] buffer) throws IOException {
    super.writeHttpRequest(buffer);
  }

  public static MockStream create(Tx tx, InputStream input, OutputStream output) throws IOException {
    var instance = new MockStream(tx, input, output);
    return instance;
  }
}
