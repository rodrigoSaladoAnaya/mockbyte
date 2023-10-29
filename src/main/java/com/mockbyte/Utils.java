package com.mockbyte;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class Utils {

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

}
