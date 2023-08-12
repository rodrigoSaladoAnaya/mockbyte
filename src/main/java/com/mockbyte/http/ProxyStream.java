package com.mockbyte.http;

import com.mockbyte.config.Config;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

public class ProxyStream implements HttpStream {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final int size = 1024;
  private final Deque<Byte> lncr = new ArrayDeque<>(2);
  private final Deque<Byte> lec = new ArrayDeque<>(4);
  private final Deque<Byte> eof = new ArrayDeque<>(7);
  private final InputStream input;
  private final OutputStream output;
  @Getter
  private final Tx tx;

  public ProxyStream(Tx tx, InputStream input, OutputStream output) {
    this.tx = tx;
    this.input = input;
    this.output = output;
  }

  public String readLine() throws IOException {
    lncr.clear();
    var buffer = new ArrayList<Byte>();
    byte read;
    while (!isLNCR()) {
      read = (byte) input.read();
      addTail(read);
      buffer.add(read);
    }
    return new String(listAsBytes(buffer), StandardCharsets.UTF_8);
  }

  public void writeCommand() throws IOException, NoSuchAlgorithmException {
    lec.clear();
    var command = new StringBuilder();
    while (!isLEC()) {
      var line = readLine();
      line = processLine(line);
      command.append(line);
    }
    tx.setCommand(command.toString());
    setTxHash();
    var buffer = tx.getCommand().getBytes(StandardCharsets.UTF_8);
    writeHttpRequest(buffer);
  }

  public void writeChunked() throws IOException {
    eof.clear();
    var buffer = new byte[size];
    var contentLength = 0;
    while (!isEOF()) {
      var read = input.read(buffer);
      contentLength += read;
      writeHttpBody(buffer, 0, read);
      addTail(buffer, read);
    }
    if (tx.getContentLength() == -1) {
      tx.setContentLength(contentLength);
    }
  }

  public void writeFixedLength() throws IOException {
    var total = 0;
    var buffer = new byte[size];
    while (total < tx.getContentLength()) {
      var read = input.read(buffer);
      writeHttpBody(buffer, 0, read);
      total += read;
    }
  }

  public void endRequest() throws IOException {
    output.flush();
  }

  public void writeHttpRequest(byte[] buffer) throws IOException {
    write(buffer, 0, buffer.length);
  }

  public void writeHttpBody(byte[] buffer, int off, int len) throws IOException {
    write(buffer, off, len);
  }

  private void write(byte[] buffer, int off, int len) throws IOException {
    output.write(buffer, off, len);
  }

  private void setTxHash() throws NoSuchAlgorithmException {
    if (tx.getType() == Tx.Type.REQ) {
      if (tx.getMkbHeader() != null && !tx.getMkbHeader().isEmpty()) {
        tx.setHash(Config.normalize(tx.getMkbHeader()));
      } else {
        tx.setHash(Config.md5(tx.getCommand()));
      }
    }
  }

  private String processLine(String line) {
    if (Stream.of("post", "get", "put", "delete", "patch").anyMatch(method -> line.toLowerCase().startsWith(method))) {
      tx.setStarLine(line.trim());
    }
    if (line.toLowerCase().startsWith("transfer-encoding: chunked")) {
      tx.setChunked(true);
    }
    if (line.toLowerCase().startsWith("content-length:")) {
      tx.setContentLength(Integer.parseInt(line.substring(line.indexOf(':') + 1).trim()));
    }
    if (line.toLowerCase().startsWith("host:")) {
      return line.replaceFirst(tx.getLocalHeaderHost(), tx.getRemoteHeaderHost());
    }
    if (line.toLowerCase().startsWith("x-mockbyte:")) {
      tx.setMkbHeader(line.substring(line.indexOf(':') + 1).trim());
      return "";
    }
    return line;
  }


  private boolean isLNCR() {
    return lncr.toString().equals("[13, 10]");
  }

  private boolean isLEC() {
    return lec.toString().equals("[13, 10, 13, 10]");
  }

  private boolean isEOF() {
    return eof.toString().equals("[13, 10, 48, 13, 10, 13, 10]");
  }

  private void addTail(byte[] buffer, int read) {
    var sizeof = 7;
    var i = read - Math.min(read, sizeof);
    for (; i < read; i++) {
      var b = buffer[i];
      addLNCR(b);
      addLEC(b);
      addEOF(b);
    }
  }

  private void addTail(byte b) {
    addLNCR(b);
    addLEC(b);
    addEOF(b);
  }

  private void addLNCR(byte b) {
    addDeque(lncr, 2, b);
  }

  private void addLEC(byte b) {
    addDeque(lec, 4, b);
  }

  private void addEOF(byte b) {
    addDeque(eof, 7, b);
  }

  private void addDeque(Deque<Byte> deque, int size, byte b) {
    if (deque.size() == size) {
      deque.removeFirst();
    }
    deque.addLast(b);
  }

  private byte[] listAsBytes(List<Byte> list) {
    var bytes = new byte[list.size()];
    for (var i = 0; i < list.size(); i++) {
      bytes[i] = list.get(i);
    }
    return bytes;
  }

  @Override
  public void close() throws IOException {
    var exception = new IOException();
    try {
      input.close();
    } catch (IOException cause) {
      exception.addSuppressed(cause);
    }
    try {
      output.close();
    } catch (IOException cause) {
      exception.addSuppressed(cause);
    }
    if (exception.getSuppressed().length > 0) {
      throw exception;
    }
  }

  public static ProxyStream create(Tx tx, InputStream input, OutputStream output) throws IOException {
    var instance = new ProxyStream(tx, input, output);
    return instance;
  }

}
