package com.mockbyte;

import com.mockbyte.psql.Config;
import com.mockbyte.psql.ProxyFlow;
import com.mockbyte.psql.RecordFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class MockByte {

  public static final Logger log = LoggerFactory.getLogger(MockByte.class);

  public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
    RecordFlow.create(Config.create());
  }

}
