package com.mockbyte.http;

import com.mockbyte.Args;
import com.mockbyte.config.ConfigHttp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Record extends Proxy {

  public Record(InputStream input, OutputStream output) {
    super(input, output);
  }

  public static Record create(Args args, ConfigHttp config, Socket localSocket) {
    return null;
  }

}
