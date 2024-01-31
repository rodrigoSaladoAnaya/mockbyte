package com.mockbyte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class Utils {

  public static Logger log = LoggerFactory.getLogger(Utils.class);

  public static String messageHash(byte[] message) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(message);
    String hexHash = bytesToHex(hash);
    return hexHash;
  }

  public static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (int i = 0; i < hash.length; i++) {
      String hex = Integer.toHexString(0xff & hash[i]);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  public static String printBytes(byte[] bytes) {
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

  public static void printMessage(String prefix, byte[] buffer) throws NoSuchAlgorithmException {
    System.out.printf("<HEX > %s%s\n", prefix, printBytes(buffer));
    System.out.printf("<BIT > %s%s\n", prefix, Arrays.toString(buffer));
    System.out.printf("<HASH> %s%s\n", prefix, messageHash(buffer));
    System.out.print("----------------------------------------\n\n");
  }

  public static byte[] joinMessage(byte[] header, byte[] tail) {
    ByteBuffer buffer = ByteBuffer.allocate(header.length + tail.length);
    buffer.put(header);
    buffer.put(tail);
    return buffer.array();
  }

  public static byte[] listToByte(List<Byte> list) {
    byte[] message = new byte[list.size()];
    for (int i = 0; i < list.size(); i++) {
      message[i] = list.get(i);
    }
    return message;
  }

  public static void addDeque(Deque<Byte> deque, int size, byte b) {
    if (deque.size() == size) {
      deque.removeFirst();
    }
    deque.addLast(b);
  }

  public static void recordMessage(byte[] message, String type) throws IOException {
    var file = new File(String.format("./mkb/%s", type));
    Files.write(file.toPath(), message);
  }

}
