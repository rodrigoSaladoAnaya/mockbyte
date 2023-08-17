package com.mockbyte;

import com.mockbyte.config.Config;
import com.mockbyte.server.ServerHttp;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class MockByte {

  private static String printBytes(byte[] bytes) {
    var hexString = new StringBuilder();
    for (byte b : bytes) {
      var ch = (char) b;
      if (b >= 32 && b <= 126) {
        hexString.append(String.format("%s", ch));
      } else {
        hexString.append(".");
      }
    }
    return hexString.toString();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.printf("++++%s\n", (2345%100));
    if(true) return;
    var log = LoggerFactory.getLogger(MockByte.class);
    var clientExit = new byte[]{88, 0, 0, 0, 4};
    var serverExit = new byte[]{83, 0, 0, 0, 4};
    var size = 1024;
    var isExit = new AtomicBoolean(false);
    Config.threadFactory.newThread(() -> {
      try (
        var cliente = new ServerSocket(4646);
        var postgresql = new Socket("localhost", 5432);
      ) {
        while (!cliente.isClosed()) {
          var frontSocket = cliente.accept();

          Config.threadFactory.newThread(() -> {
            try (
              var frontInput = frontSocket.getInputStream();
              var serverOutput = postgresql.getOutputStream();
            ) {
              byte[] buffer = new byte[size];
              int read;
              while ((read = frontInput.read(buffer)) != -1) {
                var barr = Arrays.copyOfRange(buffer, 0, read);
                isExit.set(Arrays.equals(barr, clientExit));
                System.out.printf("f->: %s\n", printBytes(barr));
                System.out.printf("f->: (%s) %s :: %s\n", read, Arrays.toString(barr), isExit);
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
              var serverInput = postgresql.getInputStream();
              var frontOutput = frontSocket.getOutputStream()
            ) {
              byte[] buffer = new byte[size];
              int read;
              while ((read = serverInput.read(buffer)) != -1) {
                var barr = Arrays.copyOfRange(buffer, 0, read);
                System.out.printf("b<-: %s\n", printBytes(barr));
                System.out.printf("b<-: (%s) %s\n", read, Arrays.toString(barr));
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

    try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:4646/mxtest?sslmode=disable", "pruebas", "test*")) {
      Statement statement = connection.createStatement();
      log.info("0)---- Wwwowoowow");
      ResultSet resultSet = statement.executeQuery("select * from movimiento limit 100");
      log.info("1)---- Wwwowoowow");
      statement.executeQuery("select 'mundo'");
      log.info("2)---- Wwwowoowow");
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
