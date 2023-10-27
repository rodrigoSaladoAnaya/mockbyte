package com.mockbyte.psql;

import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class Config {
  public static Config create() {
    return new Config();
  }
}
