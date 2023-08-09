package com.mockbyte.html;

import com.mockbyte.Config;
import lombok.Data;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@ToString
public class HTTPMetaInfo {
  public enum Type {REQ, RES}

  private Type type;
  private String localHeaderHost;
  private String remoteHeaderHost;
  private String starLine;
  private boolean chunked;
  private int contentLength = -1;
  private AtomicInteger txs = new AtomicInteger(-1);
  private String hash;
  private String dir;
  private String command;

  private HTTPMetaInfo() {
  }

  public void clearAsType(Type type) {
    this.type = type;
    contentLength = -1;
    chunked = false;
    command = null;
  }

  public void incrementTxs() {
    txs.incrementAndGet();
  }

  public static HTTPMetaInfo create(Config config) {
    var instance = new HTTPMetaInfo();
    instance.localHeaderHost = String.format("%s:%s", config.getLocalHost(), config.getLocalPort());
    if (config.isSsl()) {
      instance.remoteHeaderHost = config.getRemoteHost();
    } else {
      instance.remoteHeaderHost = String.format("%s:%s", config.getRemoteHost(), config.getRemotePort());
    }
    return instance;
  }
}
