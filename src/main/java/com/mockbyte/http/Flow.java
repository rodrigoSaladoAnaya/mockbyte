package com.mockbyte.http;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface Flow {

  void http(Tx tx, HttpStream inputStream, HttpStream outputStream) throws IOException, NoSuchAlgorithmException;

}
