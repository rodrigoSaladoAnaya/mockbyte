package com.mockbyte.http;

import java.io.InputStream;
import java.io.OutputStream;

public class Mock extends ProxyStream {

  public Mock(InputStream input, OutputStream output) {
    super(null, input, output);
  }
}
