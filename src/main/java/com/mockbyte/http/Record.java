package com.mockbyte.http;

import java.io.InputStream;
import java.io.OutputStream;

public class Record extends ProxyStream {

  public Record(InputStream input, OutputStream output) {
    super(null, input, output);
  }
}
