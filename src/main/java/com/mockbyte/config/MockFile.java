package com.mockbyte.config;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MockFile {
  private String path;
  private int delay;
}
