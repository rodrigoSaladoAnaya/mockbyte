package com.mockbyte.http;

import com.mockbyte.Args;
import com.mockbyte.config.Config;
import com.mockbyte.config.ConfigHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class RecordStream extends ProxyStream {

  private enum SUFFIX {mkb, meta}

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final Args args;
  private final ConfigHttp config;
  private final Tx tx;
  private FileOutputStream output;

  public RecordStream(Args args, ConfigHttp config, Tx tx, InputStream input, OutputStream output) {
    super(tx, input, output);
    this.args = args;
    this.config = config;
    this.tx = tx;
  }

  public void writeHttpRequest(byte[] buffer) throws IOException {
    tx.setTime(System.currentTimeMillis());
    tx.getCount().incrementAndGet();
    var file = getFile(SUFFIX.mkb);
    log.info("RECORD_START -> uuid: {}, hash: {}, file: {}", tx.getUuid(), tx.getHash(), file);
    output = new FileOutputStream(file);
    super.writeHttpRequest(buffer);
    output.write(buffer);
  }

  public void writeHttpBody(byte[] buffer, int off, int len) throws IOException {
    super.writeHttpBody(buffer, off, len);
    output.write(buffer, off, len);
  }

  public void endRequest() throws IOException {
    tx.setTime(System.currentTimeMillis() - tx.getTime());
    super.endRequest();
    output.close();
    log.info("RECORD_STOP ({}ms)-> uuid: {}, hash: {}", tx.getTime(), tx.getUuid(), tx.getHash());
    var metaFile = getFile(SUFFIX.meta);
    var json = Config.objectMapper.createObjectNode();
    json.put("elapsed", tx.getTime());
    Files.write(metaFile.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
  }

  public File getDir() {
    tx.setDir(String.format("%s/%s/%s", config.getMockDir(), Config.normalize(tx.getRemoteHeaderHost()), tx.getHash()));
    var dir = new File(tx.getDir());
    if (!dir.exists()) {
      boolean mkdirs = dir.mkdirs();
    }
    return dir;
  }

  public File getFile(SUFFIX suffix) throws IOException {
    var dir = getDir();
    var file = new File(dir, getFileName(suffix));
    if (!file.exists()) {
      boolean newFile = file.createNewFile();
    }
    return file;
  }

  public String getFileName(SUFFIX suffix) {
    return String.format("%s.%s", getTx().getCount(), suffix);
  }

  public static RecordStream create(Args args, ConfigHttp config, Tx tx, InputStream input, OutputStream output) throws IOException {
    var instance = new RecordStream(args, config, tx, input, output);
    return instance;
  }
}
