package com.mockbyte.html;

import com.mockbyte.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class HTTPRecorder {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final HTTPMetaInfo meta;
  private final Command command;
  private FileOutputStream output;
  private final byte[] EXIT = {(byte) -1};
  private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();


  private HTTPRecorder(HTTPMetaInfo meta, Command command) {
    this.meta = meta;
    this.command = command;
  }

  private File getDir() {
    meta.setDir(String.format("%s/%s/%s", "out", HTTPRecorder.dirName(meta.getRemoteHeaderHost()), meta.getHash()));
    var dir = new File(meta.getDir());
    if (!dir.exists()) {
      boolean mkdirs = dir.mkdirs();
    }
    return dir;
  }

  private File getFile() throws IOException {
    var dir = getDir();
    var file = new File(dir, String.format("%s_%s", meta.getTxs(), meta.getType()));
    if (!file.exists()) {
      boolean newFile = file.createNewFile();
    }
    return file;
  }

  public void start() throws IOException {
    if (command != Command.RECORD) {
      return;
    }
    output = new FileOutputStream(getFile());
    Thread.ofVirtual()
      .name("mockbyte-http-recorder-", 0)
      .start(() -> {
        try {
          while (true) {
            byte[] buffer = queue.take();
            if (Arrays.equals(buffer, EXIT) && queue.isEmpty()) {
              break;
            }
            log.trace("REC -> {}", Arrays.toString(buffer));
            output.write(buffer);
          }
        } catch (IOException | InterruptedException ex) {
          log.error("Error during recording", ex);
        }
      });
  }

  public void write(byte[] buffer, int off, int read) {
    if (command != Command.RECORD) {
      return;
    }
    byte[] bytes = new byte[read];
    System.arraycopy(buffer, off, bytes, 0, read);
    queue.add(bytes);
  }

  public void stop() throws InterruptedException {
    if (command != Command.RECORD) {
      return;
    }
    queue.put(EXIT);
  }

  public static HTTPRecorder create(HTTPMetaInfo meta, Command command) {
    var instance = new HTTPRecorder(meta, command);
    return instance;
  }

  public static String dirName(String remoteHeaderHost) {
    var name = remoteHeaderHost.replace(':', '_');
    name = name.replace('.', '_');
    name = name.toLowerCase();
    name = name.replaceAll("\\s+", " ");
    var normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
    normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    return normalized;
  }

  public static String md5(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashInBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hashInBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

}
