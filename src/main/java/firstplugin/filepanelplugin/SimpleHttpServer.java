package firstplugin.filepanelplugin;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleHttpServer {
    private static HttpServer server;

    public static void start(int port, String validKey, File rootDir) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            System.out.println("HTTP Server started on port " + port);

            server.createContext("/files", exchange -> {
                try {
                    System.out.println("Received request: " + exchange.getRequestURI());

                    String query = exchange.getRequestURI().getQuery();
                    String keyParam = null;
                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] parts = param.split("=");
                            if (parts.length == 2 && parts[0].equals("key")) {
                                keyParam = parts[1];
                                break;
                            }
                        }
                    }

                    if (!validKey.equals(keyParam)) {
                        exchange.sendResponseHeaders(403, 0);
                        exchange.getResponseBody().close();
                        System.out.println("Invalid key attempt: " + keyParam);
                        return;
                    }

                    System.out.println("Root directory: " + rootDir.getAbsolutePath());
                    System.out.println("Root directory exists: " + rootDir.exists());
                    System.out.println("Root directory readable: " + rootDir.canRead());

                    if (!rootDir.exists() || !rootDir.canRead()) {
                        exchange.sendResponseHeaders(500, 0);
                        exchange.getResponseBody().close();
                        System.out.println("Root directory not accessible");
                        return;
                    }

                    // Lấy tất cả file .yml hoặc .properties trong rootDir đệ quy
                    List<File> files = listFilesRecursive(rootDir);

                    // Chuyển file thành đường dẫn tương đối so với rootDir, để dễ xem hơn
                    List<String> relativePaths = files.stream()
                            .map(file -> rootDir.toURI().relativize(file.toURI()).getPath())
                            .collect(Collectors.toList());

                    String json = relativePaths.toString();

                    byte[] response = json.getBytes();
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length);

                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();

                    System.out.println("Returned file list: " + json);

                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        exchange.sendResponseHeaders(500, 0);
                        exchange.getResponseBody().close();
                    } catch (IOException ignored) {}
                }
            });

            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<File> listFilesRecursive(File dir) {
        List<File> result = new ArrayList<>();
        if (dir == null || !dir.exists()) return result;
        File[] files = dir.listFiles();
        if (files == null) return result;

        for (File f : files) {
            if (f.isDirectory()) {
                result.addAll(listFilesRecursive(f));
            } else if (f.getName().endsWith(".yml") || f.getName().endsWith(".properties")) {
                result.add(f);
            }
        }
        return result;
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP Server stopped");
        }
    }
}
