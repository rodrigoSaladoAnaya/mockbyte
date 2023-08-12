package com.mockbyte.http;

import com.mockbyte.config.ConfigHttp;
import lombok.ToString;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@lombok.Data
@ToString
public class Tx {
  public enum Type {REQ, RES}

  private String uuid = UUID.randomUUID().toString();
  private Type type;
  private String localHeaderHost;
  private String remoteHeaderHost;
  private String starLine;
  private boolean chunked;
  private int contentLength = -1;
  private String mkbHeader;
  private String hash;
  @ToString.Exclude
  private String command;
  @ToString.Exclude
  private String dir;
  @ToString.Exclude
  private AtomicInteger txs = new AtomicInteger(-1);

  public static Tx create(ConfigHttp config) {
    var instance = new Tx();
    instance.localHeaderHost = String.format("%s:%s", config.getLocalHost(), config.getLocalPort());
    if (config.isSsl()) {
      instance.remoteHeaderHost = config.getRemoteHost();
    } else {
      instance.remoteHeaderHost = String.format("%s:%s", config.getRemoteHost(), config.getRemotePort());
    }
    return instance;
  }
}