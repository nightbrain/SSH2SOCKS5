import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class App {
    private static ExecutorService executorService;

    public static void main(String[] args) throws IOException {
        int port = 2223;
        int maxPool = 100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            maxPool = Integer.parseInt(args[1]);
        }
        System.out.println("Port: " + port + ";Max Pool: " + maxPool);
        executorService = Executors.newFixedThreadPool(maxPool);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/connect", App::connect);
        httpServer.setExecutor(null);
        httpServer.start();
    }

    public static void connect(HttpExchange httpExchange) throws IOException {
        Future<String> future = executorService.submit(
            () -> {
                SSH ssh = new SSH();
                return "Hello!";
            }
        );
        String response = "ERROR";
        try {
            response = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
