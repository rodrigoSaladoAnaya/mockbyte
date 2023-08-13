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
import java.util.*;
import java.util.stream.Stream;

public class ProxyStream implements HttpStream {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final int size = 1024;
  private final byte[] END_OF_LINE = {13, 10};
  private final byte[] END_OF_BLOCK = {13, 10, 13, 10};
  private final byte[] END_OF_FILE = {13, 10, 48, 13, 10, 13, 10};
  private final Deque<Byte> eol = new ArrayDeque<>(END_OF_LINE.length);
  private final Deque<Byte> eob = new ArrayDeque<>(END_OF_BLOCK.length);
  private final Deque<Byte> eof = new ArrayDeque<>(END_OF_FILE.length);
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
    eol.clear();
    var buffer = new ArrayList<Byte>();
    byte read;
    while (!isEOL()) {
      read = (byte) input.read();
      addTail(read);
      buffer.add(read);
    }
    return new String(listAsBytes(buffer), StandardCharsets.UTF_8);
  }

  public void writeCommand() throws IOException, NoSuchAlgorithmException {
    eob.clear();
    var command = new StringBuilder();
    while (!isEOB()) {
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
    tx.setTime(System.currentTimeMillis() - tx.getTime());///Duuuu maybe
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
        tx.setHash(Config.md5(tx.getStarLine()));
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

  private boolean isEOL() {
    return eol.toString().equals(Arrays.toString(END_OF_LINE));
  }

  private boolean isEOB() {
    return eob.toString().equals(Arrays.toString(END_OF_BLOCK));
  }

  private boolean isEOF() {
    return eof.toString().equals(Arrays.toString(END_OF_FILE));
  }

  private void addTail(byte[] buffer, int read) {
    var sizeof = END_OF_FILE.length;
    var i = read - Math.min(read, sizeof);
    for (; i < read; i++) {
      var b = buffer[i];
      addEOL(b);
      addEOB(b);
      addEOF(b);
    }
  }

  private void addTail(byte b) {
    addEOL(b);
    addEOB(b);
    addEOF(b);
  }

  private void addEOL(byte b) {
    addDeque(eol, END_OF_LINE.length, b);
  }

  private void addEOB(byte b) {
    addDeque(eob, END_OF_BLOCK.length, b);
  }

  private void addEOF(byte b) {
    addDeque(eof, END_OF_FILE.length, b);
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
