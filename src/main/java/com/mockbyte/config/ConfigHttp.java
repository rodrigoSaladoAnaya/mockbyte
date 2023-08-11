package com.mockbyte.config;

import com.mockbyte.Args;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public final class ConfigHttp implements Config {
  private Args.ServerType serverType;
  private String localHost;
  private int localPort;
  private String remoteHost;
  private int remotePort;
  private boolean ssl;
  private String mockDir = "./mkb";
}
