package com.mockbyte.psql;

import java.io.IOException;
import java.io.InputStream;

public class ProxyInputStream extends InputStream {

  @Override
  public int read() throws IOException {
    return 0;
  }

}
