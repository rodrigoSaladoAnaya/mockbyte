package com.mockbyte.html;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

public class HTTPStream implements Closeable {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final int size = 1024;
  private final Deque<Byte> lncr = new ArrayDeque<>(2);
  private final Deque<Byte> lec = new ArrayDeque<>(4);
  private final Deque<Byte> eof = new ArrayDeque<>(7);
  private final HTTPMetaInfo meta;
  private final InputStream input;
  private final OutputStream output;
  private final HTTPRecorder recorder;

  public HTTPStream(HTTPMetaInfo meta, InputStream input, OutputStream output, HTTPRecorder recorder) {
    this.meta = meta;
    this.input = input;
    this.output = output;
    this.recorder = recorder;
  }

  public void writeCommand() throws IOException {
    lec.clear();
    var command = new StringBuilder();
    while (!isLEC()) {
      var line = readLine();
      line = processLine(line);
      command.append(line);
    }
    var buffer = command.toString().getBytes(StandardCharsets.UTF_8);
    recorder.start();
    write(buffer);
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

  public void writeChunked() throws IOException {
    eof.clear();
    var buffer = new byte[size];
    while (!isEOF()) {
      var read = input.read(buffer);
      write(buffer, 0, read);
      addTail(buffer, read);
    }
  }

  public void writeFixedLength() throws IOException {
    var total = 0;
    var buffer = new byte[size];
    while (total < meta.getContentLength()) {
      var read = input.read(buffer);
      write(buffer, 0, read);
      total += read;
    }
  }

  public void flush() throws IOException, InterruptedException {
    output.flush();
    recorder.stop();
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

  private String processLine(String line) {
    if (Stream.of("post", "get", "put").anyMatch(method -> line.toLowerCase().startsWith(method))) {
      meta.setStarLine(line.trim());
      meta.setHash(HTTPRecorder.md5(line.trim()));
    }
    if (line.toLowerCase().startsWith("host: ")) {
      return line.replaceFirst(meta.getLocalHeaderHost(), meta.getRemoteHeaderHost());
    }
    if (line.toLowerCase().startsWith("transfer-encoding: chunked")) {
      meta.setChunked(true);
    }
    if (line.toLowerCase().startsWith("content-length:")) {
      meta.setContentLength(Integer.parseInt(line.substring(line.indexOf(':') + 1).trim()));
    }
    return line;
  }

  public void show() {
    System.out.printf("\nlncr (%s)-> %s\n", lncr.size(), lncr);
    System.out.printf("\nlec (%s)-> %s\n", lec.size(), lec);
    System.out.printf("\neof (%s)-> %s\n", eof.size(), eof);
  }

  private void write(byte[] buffer) throws IOException {
    write(buffer, 0, buffer.length);
  }

  private void write(byte[] buffer, int off, int len) throws IOException {
    output.write(buffer, off, len);
    recorder.write(buffer, off, len);
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

}
