package com.mockbyte.psql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;

import static com.mockbyte.Utils.*;

public class RecordFlow {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final Config config;
  private final byte[] READY_FOR_QUERY = {90, 0, 0, 0, 5, 73};
  private final byte[] SYNC = {83, 0, 0, 0, 4};
  private final byte[] TERMINATE = {88, 0, 0, 0, 4};
  private final Deque<Byte> rfq = new ArrayDeque<>(READY_FOR_QUERY.length);
  private final Deque<Byte> sync = new ArrayDeque<>(SYNC.length);
  private final Deque<Byte> terminate = new ArrayDeque<>(TERMINATE.length);

  private RecordFlow(Config config) {
    this.config = config;
  }

  private void clear() {
    rfq.clear();
    sync.clear();
    terminate.clear();
  }

  private boolean isRFQ() {
    return rfq.toString().equals(Arrays.toString(READY_FOR_QUERY));
  }

  private boolean isSYNC() {
    return sync.toString().equals(Arrays.toString(SYNC));
  }

  private boolean isTERMINATE() {
    var is = terminate.toString().equals(Arrays.toString(TERMINATE));
    if (!is) {
      terminate.clear();
    }
    return is;
  }

  private void addBackendTail(byte b) {
    addDeque(rfq, READY_FOR_QUERY.length, b);
  }

  private void addFrontendTail(byte b) {
    addDeque(sync, SYNC.length, b);
    addDeque(terminate, TERMINATE.length, b);
  }

  public int getAuthLength(byte[] messageBytes) {
    if (messageBytes == null || messageBytes.length < 4) {
      throw new IllegalArgumentException("Tamaño del mensaje inválido");
    }
    ByteBuffer buffer = ByteBuffer.wrap(messageBytes, 0, 4);
    return buffer.getInt();
  }

  public int getMessageLength(byte[] messageBytes) {
    if (messageBytes == null || messageBytes.length < 5) {
      throw new IllegalArgumentException("Tamaño del mensaje inválido");
    }
    ByteBuffer buffer = ByteBuffer.wrap(messageBytes, 1, 4);
    return buffer.getInt();
  }

  public int getPayloadLength(byte[] messageBytes) {
    return getMessageLength(messageBytes) - 8;
  }

  public int getMessageType(byte[] messageBytes) {
    if (messageBytes == null || messageBytes.length < 9) {
      throw new IllegalArgumentException("Mensaje inválido o demasiado corto");
    }
    ByteBuffer buffer = ByteBuffer.wrap(messageBytes, 5, 4);
    return buffer.getInt();
  }

  public byte[] frontendStartupMessage(InputStream frontendInputStream, OutputStream backendOutputStream) throws IOException {
    var header = new byte[4];
    frontendInputStream.read(header);
    var authLength = getAuthLength(header);
    var payload = new byte[authLength - header.length];
    frontendInputStream.read(payload);
    var message = joinMessage(header, payload);
    recordMessage(message, "startup");
    backendOutputStream.write(message);
    return message;
  }

  public byte[] backendAuthenticationRequest(InputStream backendInputStream, OutputStream frontendOutputStream) throws IOException {
    var header = new byte[9];
    backendInputStream.read(header);
    var payloadLength = getPayloadLength(header);
    var payload = new byte[payloadLength];
    backendInputStream.read(payload);
    var message = joinMessage(header, payload);
    recordMessage(message, "auth_req");
    frontendOutputStream.write(message);
    return message;
  }

  public byte[] frontendAuthenticationResponse(InputStream frontendInputStream, OutputStream backendOutputStream) throws IOException {
    var header = new byte[5];
    frontendInputStream.read(header);
    var authLength = getMessageLength(header);
    var payload = new byte[authLength - header.length + 1];
    frontendInputStream.read(payload);
    var message = joinMessage(header, payload);
    recordMessage(message, "auth_res");
    backendOutputStream.write(message);
    return message;
  }

  public byte[] backendMessage(InputStream backendInputStream, OutputStream frontendOutputStream, int index) throws IOException {
    int read;
    byte[] message;
    var byteList = new ArrayList<Byte>();
    while (!isRFQ()) {
      read = backendInputStream.read();
      if (read == -1) {
        break;
      }
      byte b = (byte) read;
      byteList.add(b);
      addBackendTail(b);
    }
    rfq.clear();
    message = listToByte(byteList);
    recordMessage(message, String.format("backend_message_%s", index));
    frontendOutputStream.write(message);
    return message;
  }

  public byte[] frontendMessage(InputStream frontendInputStream, OutputStream backendOutputStream, int index) throws IOException {
    int read;
    byte[] message;
    var byteList = new ArrayList<Byte>();
    while (!isSYNC()) {
      read = frontendInputStream.read();
      if (read == -1) {
        break;
      }
      byte b = (byte) read;
      byteList.add(b);
      addFrontendTail(b);
    }
    sync.clear();
    message = listToByte(byteList);
    recordMessage(message, String.format("frontend_message_%s", index));
    backendOutputStream.write(message);
    return message;
  }

  private void loop(FrontendServer frontendServer) throws IOException, NoSuchAlgorithmException {
    while (!frontendServer.isClosed()) {
      try (
        var frontendConnection = frontendServer.getNewConnection();
        var frontendInputStream = frontendConnection.getInputStream();
        var frontendOutputStream = frontendConnection.getOutputStream();
        var backendServer = PsqlBackendServer.create(config);
        var backendInputStream = backendServer.getInputStream();
        var backendOutputStream = backendServer.getOutputStream();
      ) {
        clear();
        byte[] message;

        message = frontendStartupMessage(frontendInputStream, backendOutputStream);
        printMessage("F >> ", message);

        message = backendAuthenticationRequest(backendInputStream, frontendOutputStream);
        printMessage("B << ", message);

        message = frontendAuthenticationResponse(frontendInputStream, backendOutputStream);
        printMessage("F >> ", message);

        var index = 0;
        while (!isTERMINATE()) {
          index++;
          message = backendMessage(backendInputStream, frontendOutputStream, index);
          printMessage("B << ", message);

          message = frontendMessage(frontendInputStream, backendOutputStream, index);
          printMessage("F >> ", message);
        }
        /**/
      }
    }
  }

  public static void create(Config config) throws IOException, NoSuchAlgorithmException {
    var instance = new RecordFlow(config);
    try (
      var frontendServer = FrontendServer.create(config)
    ) {
      instance.loop(frontendServer);
    }
  }

}
