package com.mockbyte.http;

import java.io.Closeable;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface HttpStream extends Closeable {
  void writeCommand() throws IOException, NoSuchAlgorithmException;

  void writeChunked() throws IOException;

  void writeFixedLength() throws IOException;

  void endRequest() throws IOException;
}
