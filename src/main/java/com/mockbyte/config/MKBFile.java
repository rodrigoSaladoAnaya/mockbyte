package com.mockbyte.config;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MKBFile {
  private String path;
  private int delay;
}
