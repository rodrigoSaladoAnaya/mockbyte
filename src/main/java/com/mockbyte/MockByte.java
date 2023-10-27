package com.mockbyte;

import com.mockbyte.psql.Config;
import com.mockbyte.psql.ProxyServer;
import com.mockbyte.psql.PsqlServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class MockByte {

  public static final Logger log = LoggerFactory.getLogger(MockByte.class);
  private static final int size = 1024;

  private static String printBytes(byte[] bytes) {
    var hexString = new StringBuilder();
    for (byte b : bytes) {
      var ch = (char) b;
      if (b >= 32 && b <= 126) {
        hexString.append(String.format("%s", ch));
      } else {
        hexString.append(String.format("[%02X]", b));
      }
    }
    return hexString.toString();
  }

  public static void printMessage(String prefix, byte[] buffer) {
    System.out.printf("<HEX> %s%s\n", prefix, printBytes(buffer));
    System.out.printf("<BIT> %s%s\n", prefix, Arrays.toString(buffer));
    System.out.print("----------------------------------------\n\n");
  }

  public static int getAuthLength(byte[] messageBytes) {
    if (messageBytes == null || messageBytes.length < 4) {
      throw new IllegalArgumentException("Tamaño del mensaje inválido");
    }
    ByteBuffer buffer = ByteBuffer.wrap(messageBytes, 0, 4);
    return buffer.getInt();
  }


  public static int getMessageLength(byte[] messageBytes) {
    if (messageBytes == null || messageBytes.length < 5) {
      throw new IllegalArgumentException("Tamaño del mensaje inválido");
    }
    ByteBuffer buffer = ByteBuffer.wrap(messageBytes, 1, 4);
    return buffer.getInt();
  }

  public static int getPayloadLength(byte[] messageBytes) {
    return getMessageLength(messageBytes) - 8;
  }

  public static int getMessageType(byte[] messageBytes) {
    if (messageBytes == null || messageBytes.length < 9) {
      throw new IllegalArgumentException("Mensaje inválido o demasiado corto");
    }
    ByteBuffer buffer = ByteBuffer.wrap(messageBytes, 5, 4);
    return buffer.getInt();
  }

  /**
   * Considerar:
   * Current backend transaction status indicator. Possible values are 'I' if idle (not in a transaction block); 'T' if in a transaction block; or 'E' if in a failed transaction block (queries will be rejected until block is ended).
   */
  private static final byte[] READY_FOR_QUERY = {90, 0, 0, 0, 5, 73};
  private static final byte[] SYNC = {83, 0, 0, 0, 4};
  private static final Deque<Byte> rfq = new ArrayDeque<>(READY_FOR_QUERY.length);
  private static final Deque<Byte> sync = new ArrayDeque<>(SYNC.length);

  private static boolean isRFQ() {
    return rfq.toString().equals(Arrays.toString(READY_FOR_QUERY));
  }

  private static boolean isSYNC() {
    return sync.toString().equals(Arrays.toString(SYNC));
  }

  private static void addDeque(Deque<Byte> deque, int size, byte b) {
    if (deque.size() == size) {
      deque.removeFirst();
    }
    deque.addLast(b);
  }

  private static void addRFQ(byte b) {
    addDeque(rfq, READY_FOR_QUERY.length, b);
  }

  private static void addSYNC(byte b) {
    addDeque(sync, SYNC.length, b);
  }

  private static void addTailRFQ(byte b) {
    addRFQ(b);
  }

  private static void addTailSYNC(byte b) {
    addSYNC(b);
  }

  public static byte[] joinMessage(byte[] header, byte[] payload) {
    ByteBuffer authMessage = ByteBuffer.allocate(header.length + payload.length);
    authMessage.put(header);
    authMessage.put(payload);
    return authMessage.array();
  }

  public static byte[] listToByte(List<Byte> list) {
    byte[] message = new byte[list.size()];
    for (int i = 0; i < list.size(); i++) {
      message[i] = list.get(i);
    }
    return message;
  }

  public static void backendMessage(InputStream input, OutputStream output) throws IOException {
    int read;
    byte[] message;
    List<Byte> byteList = new ArrayList<>();
    while (!isRFQ()) {
      read = input.read();
      if (read == -1) {
        break;
      }
      byte b = (byte) read;
      byteList.add(b);
      addTailRFQ(b);
    }
    rfq.clear();
    message = listToByte(byteList);
    printMessage("B << ", message);
    output.write(message);
  }

  public static void frontendMessage(InputStream input, OutputStream output) throws IOException {
    int read;
    byte[] message;
    List<Byte> byteList = new ArrayList<>();
    while (!isSYNC()) {
      read = input.read();
      if (read == -1) {
        break;
      }
      byte b = (byte) read;
      byteList.add(b);
      addTailSYNC(b);
    }
    sync.clear();
    message = listToByte(byteList);
    printMessage("F >> ", message);
    output.write(message);
  }

  public static void main(String[] args) throws IOException {
    try (
      var proxy = ProxyServer.create(Config.create());
    ) {
      while (!proxy.getServerSocket().isClosed()) {
        var proxyConnection = proxy.getNewConnection();
        try (
          var psql = PsqlServer.create(Config.create());
          var psqlInputStream = psql.getSocket().getInputStream();
          var psqlOutputStream = psql.getSocket().getOutputStream();
          var proxyInputStream = proxyConnection.getInputStream();
          var proxyOutputStream = proxyConnection.getOutputStream();
        ) {
          int read;
          byte[] header;
          byte[] payload;
          byte[] message;
          int headerLength;

          header = new byte[4];
          proxyInputStream.read(header);
          headerLength = getAuthLength(header);
          payload = new byte[headerLength - header.length];
          proxyInputStream.read(payload);
          message = joinMessage(header, payload);
          printMessage("F >> ", message);
          psqlOutputStream.write(message);

          header = new byte[9];
          psqlInputStream.read(header);
          var length = getPayloadLength(header);
          payload = new byte[length];
          psqlInputStream.read(payload);
          message = joinMessage(header, payload);
          printMessage("B << ", message);
          proxyOutputStream.write(message);

          header = new byte[5];
          proxyInputStream.read(header);
          headerLength = getMessageLength(header);
          payload = new byte[headerLength - header.length + 1];
          proxyInputStream.read(payload);
          message = joinMessage(header, payload);
          printMessage("F >> ", message);
          psqlOutputStream.write(message);


          backendMessage(psqlInputStream, proxyOutputStream);
          frontendMessage(proxyInputStream, psqlOutputStream);

          backendMessage(psqlInputStream, proxyOutputStream);
          frontendMessage(proxyInputStream, psqlOutputStream);

          backendMessage(psqlInputStream, proxyOutputStream);
          frontendMessage(proxyInputStream, psqlOutputStream);

          backendMessage(psqlInputStream, proxyOutputStream);
          frontendMessage(proxyInputStream, psqlOutputStream);

          log.info("FIN....");
        }
      }
    }
  }

}
