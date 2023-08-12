package com.mockbyte;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Args {

  public enum ServerType {HTTP, POSTGRESQL, ISO8583, GRPC}

  public enum Command {PROXY, RECORD, MOCK}

  private String configPath;
  private ServerType serverType;
  private Command command;

  public static Args create(String[] args) {
    var instance = new Args();
    instance.setConfigPath(args[0]);
    instance.setServerType(ServerType.valueOf(args[1].toUpperCase()));
    instance.setCommand(Command.valueOf(args[2].toUpperCase()));
    return instance;
  }

}
