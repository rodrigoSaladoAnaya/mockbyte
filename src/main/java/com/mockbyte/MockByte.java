package com.mockbyte;

import com.mockbyte.config.Config;
import com.mockbyte.server.ServerHttp;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

public class MockByte {

  /**
   * Considerar:
   * Current backend transaction status indicator. Possible values are 'I' if idle (not in a transaction block); 'T' if in a transaction block; or 'E' if in a failed transaction block (queries will be rejected until block is ended).
   */
  private static final byte[] READY_FOR_QUERY = {90, 0, 0, 0, 5, 73};
  private static final Deque<Byte> rfq = new ArrayDeque<>(READY_FOR_QUERY.length);

  private static boolean isRFQ() {
    return rfq.toString().equals(Arrays.toString(READY_FOR_QUERY));
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

  private static void addTail(byte[] buffer, int read) {
    var sizeof = READY_FOR_QUERY.length;
    var i = read - Math.min(read, sizeof);
    for (; i < read; i++) {
      var b = buffer[i];
      addRFQ(b);
    }
  }

  private static void addTail(byte b) {
    addRFQ(b);
  }

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

  public static void sendAuthMessage(InputStream backendInput, OutputStream frontOutput) throws IOException {
    byte[] header = new byte[9];
    var read = backendInput.read(header);
    var length = getPayloadLength(header);
    byte[] payload = new byte[length];
    read = backendInput.read(payload);

    ByteBuffer buffer = ByteBuffer.allocate(header.length + payload.length);
    buffer.put(header);
    buffer.put(payload);
    byte[] response = buffer.array();
    printMessage("BACK <-", response);
    frontOutput.write(response);
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    var log = LoggerFactory.getLogger(MockByte.class);
    var clientExit = new byte[]{88, 0, 0, 0, 4};
    var serverExit = new byte[]{83, 0, 0, 0, 4};
    var size = 1024;
    var isExit = new AtomicBoolean(false);
    Config.threadFactory.newThread(() -> {
      try (
        var proxy = new ServerSocket(4646);
        var postgresql = new Socket("localhost", 5432);
      ) {
        while (!proxy.isClosed()) {
          var frontendSocket = proxy.accept();
          Config.threadFactory.newThread(() -> {
            try (
              var frontInput = frontendSocket.getInputStream();
              var serverOutput = postgresql.getOutputStream();
            ) {
              byte[] buffer = new byte[size];
              int read;
              while ((read = frontInput.read(buffer)) != -1) {
                var barr = Arrays.copyOfRange(buffer, 0, read);
                isExit.set(Arrays.equals(barr, clientExit));
                printMessage("FONT ->", barr);
                serverOutput.write(buffer, 0, read);
                if (isExit.get()) {
                  break;
                }
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }).start();


          Config.threadFactory.newThread(() -> {
            try (
              var backendInput = postgresql.getInputStream();
              var frontOutput = frontendSocket.getOutputStream()
            ) {
              sendAuthMessage(backendInput, frontOutput);

              byte[] buffer = new byte[size];
              int read;
              while ((read = backendInput.read(buffer)) != -1) {
                var barr = Arrays.copyOfRange(buffer, 0, read);
                addTail(buffer, read);
                printMessage("BACK <-", barr);
                log.info("READY_FOR_QUERY <{}> {}", rfq, isRFQ());
                if (isRFQ()) {
                  rfq.clear();
                }
                frontOutput.write(buffer, 0, read);
              }
            } catch (IOException e) {
              log.error("Fuck :", e);
            }
          }).start();

        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    }).start();

    Thread.sleep(4000);

    try (
      Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:4646/mxtest?sslmode=disable", "pruebas", "test*");
      Statement statement = connection.createStatement();
    ) {
      statement.executeQuery("select * from canal_mov limit 100");
      statement.execute("select 'mundo'");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private MockByte(Args args, Config config) throws IOException {
    var server = switch (args.getServerType()) {
      case HTTP -> ServerHttp.create(args, config);
      case POSTGRESQL -> null;
      case ISO8583 -> null;
      case GRPC -> null;
    };
    server.start();
  }

  private static void createDefault(String[] arr) throws IOException {
    var args = Args.create(arr);
    var config = Config.create(args);
    new MockByte(args, config);
  }

}
