package com.mockbyte.http;

import com.mockbyte.config.ConfigHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

public class MockStream extends ProxyStream {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final ConfigHttp config;
  private final Tx tx;
  private final InputStream input;
  private final OutputStream output;

  public MockStream(ConfigHttp config, Tx tx, InputStream input, OutputStream output) {
    super(tx, input, output);
    this.config = config;
    this.tx = tx;
    this.input = input;
    this.output = output;
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

  public static MockStream create(ConfigHttp config, Tx tx, InputStream input, OutputStream output) throws IOException {
    var instance = new MockStream(config, tx, input, output);
    return instance;
  }
}
