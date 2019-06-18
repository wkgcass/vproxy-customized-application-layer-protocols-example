package net.cassite.vproxy_application_layer_protocol_example;

import vproxy.processor.DefaultProcessorRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    private static final int lbPort = 7890;
    private static final int server1 = 17891;
    private static final int server2 = 17892;

    public static void main(String[] args) throws Exception {
        serverStart(server1);
        serverStart(server2);
        vproxyStart();
        Thread.sleep(1500); // wait for health check
        clientStart();

        System.exit(0);
    }

    private static String read(byte[] head, InputStream input) throws Exception {
        int read;
        try {
            read = input.read(head);
        } catch (IOException e) {
            return null;
        }
        if (read < 0)
            return null;
        assert read == 3; // the packet is small so all bytes it should arrive at the same time
        int len = ((head[0] & 0xff) << 16) | ((head[1] & 0xff) << 8) | (head[2] & 0xff);
        byte[] payload = new byte[len];
        int off = 0;
        while (off != len) {
            read = input.read(payload, off, len - off);
            if (read < 0)
                return null;
            off += read;
        }
        return new String(payload, 0, len, StandardCharsets.UTF_8);
    }

    private static void serverStart(int port) {
        new Thread(() -> {
            try {
                ServerSocket socket = new ServerSocket();
                socket.bind(new InetSocketAddress("127.0.0.1", port));
                while (true) {
                    Socket sock = socket.accept();
                    if (sock == null)
                        break;
                    new Thread(() -> {
                        byte[] head = new byte[3];
                        try {
                            while (true) {
                                InputStream input = sock.getInputStream();
                                OutputStream output = sock.getOutputStream();
                                String payload = read(head, input);
                                if (payload == null)
                                    break;

                                String resp = "echo from server " + port + ": " + payload;
                                byte[] bytes = Serializer.serialize(resp);
                                output.write(bytes);
                                output.flush();
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }).start();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }).start();
    }

    private static void vproxyStart() throws Exception {
        // A work around for packing everything into one jar
        // When using the processor impl as a module, the ServiceLoader can
        // find and load the impl according to the `provides` statement
        // in module-info.java
        // However it might not work when packing the impl along with the vproxy
        // into one fat jar.
        DefaultProcessorRegistry.getInstance().register(new ExampleProcessor());

        // start vproxy, you may directly pass `args` to vproxy Main when using
        vproxy.app.Main.main(new String[]{
            "resp-controller", "127.0.0.1:16379", "mypassw0rd",
            "noStdIOController",
            "sigIntDirectlyShutdown",
            "noLoadLast",
            "noSave"
        });

        // configure the vproxy with socket
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 16379));
        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();

        byte[] read = new byte[128];
        for (String cmd : Arrays.asList(
            "AUTH mypassw0rd",
            "add event-loop-group elg0",
            "add event-loop el0 to event-loop-group elg0",
            "add server-groups sgs0",
            "add server-group sg0 timeout 1000 period 500 up 2 down 3 event-loop-group elg0",
            "add server-group sg0 to server-groups sgs0 weight 10",
            "add server svr1 to server-group sg0 address 127.0.0.1:" + server1 + " weight 10",
            "add server svr2 to server-group sg0 address 127.0.0.1:" + server2 + " weight 10",
            "add tcp-lb lb0 acceptor-elg elg0 event-loop-group elg0 address 127.0.0.1:" + lbPort + " server-groups sgs0 " +
                "protocol example"
        )) {
            output.write((cmd + "\r\n").getBytes());
            output.flush();
            int len = input.read(read);
            System.out.println("configure request : " + cmd);
            System.out.println("configure response: " + new String(read, 0, len, StandardCharsets.UTF_8).trim());
        }
    }

    private static void clientStart() throws Exception {
        Scanner scanner = new Scanner(System.in);

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", lbPort));
        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();

        System.out.println();
        System.out.println("--------------------------------------------------------");
        System.out.println("This example starts two echo servers, one client and the vproxy tcp-lb with customized protocol loaded and configured.");
        System.out.println("You can input anything to the client, and the server will echo your data with its server prefix.");
        System.out.println("This example uses a very simple customized application level protocol:");
        System.out.println("+-------------+-----------------------------+");
        System.out.println("| Length (24) |      Payload (Length)       |");
        System.out.println("+-------------+-----------------------------+");
        System.out.println("The processor impl can be found in ExampleProcessor.java.");
        System.out.println("--------------------------------------------------------");
        System.out.println("Type in anything, then you will get a reply from one of the backend servers.");
        System.out.println();

        byte[] head = new byte[3];

        while (true) {
            System.out.print("input> ");
            String line = scanner.nextLine();
            if (line == null)
                break;
            if (line.equals("quit") || line.equals("exit"))
                break;
            output.write(Serializer.serialize(line));
            output.flush();

            String payload = read(head, input);
            if (payload == null)
                break;
            System.out.println(payload);
        }
    }
}
