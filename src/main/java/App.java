import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class App {
    private static ExecutorService executorService;
    private static HashMap<String, SSH> sshes = new HashMap<>();

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
        httpServer.createContext("/disconnect", App::disconnect);
        httpServer.createContext("/clear", App::clear);
        httpServer.setExecutor(null);
        httpServer.start();
    }

    private static void connect(HttpExchange httpExchange) throws IOException {
        long startTime = System.currentTimeMillis();
        JSONObject body = getRequestBody(httpExchange);
        Future<SSH> future = executorService.submit(
            () -> {
                SSH ssh = new SSH();
                boolean isConnected = ssh.connect(
                    body.get("host").toString(),
                    body.get("username").toString(),
                    body.get("password").toString()
                );
                if (isConnected) {
                    return ssh;
                }
                ssh.disconnect();
                return null;
            }
        );
        JSONObject jsonObject = new JSONObject();
        try {
            SSH ssh = future.get();
            if (ssh != null) {
                String uuid = UUID.randomUUID().toString();
                jsonObject.put("status", "OK");
                jsonObject.put("id", uuid);
                jsonObject.put("host", "127.0.0.1");
                jsonObject.put("port", ssh.getPort());
                jsonObject.put("ip", ssh.ip);
                jsonObject.put("address", "socks5://127.0.0.1:" + ssh.getPort());
                jsonObject.put("delta_time", System.currentTimeMillis() - startTime);
                sshes.put(uuid, ssh);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (!jsonObject.containsKey("status")) {
            jsonObject.put("status", "ERROR");
        }
        String response = jsonObject.toJSONString();
        httpExchange.sendResponseHeaders(jsonObject.get("status").equals("OK") ? 200 : 400, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static void disconnect(HttpExchange httpExchange) throws IOException {
        JSONObject body = getRequestBody(httpExchange);
        String id = body.get("id").toString();
        JSONObject jsonObject = new JSONObject();
        if (sshes.containsKey(id)) {
            sshes.get(id).disconnect();
            sshes.remove(id);
            jsonObject.put("status", "OK");
        } else {
            jsonObject.put("status", "ERROR");
        }
        String response = jsonObject.toJSONString();
        httpExchange.sendResponseHeaders(jsonObject.get("status").equals("OK") ? 200 : 400, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static void clear(HttpExchange httpExchange) throws IOException {
        Set<Map.Entry<String, SSH>> set = sshes.entrySet();
        for (Map.Entry<String, SSH> ssh : set) {
            ssh.getValue().disconnect();
        }
        set.clear();
        sshes.clear();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "OK");
        String response = jsonObject.toJSONString();
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static JSONObject getRequestBody(HttpExchange httpExchange) {
        StringBuilder body = new StringBuilder();
        try {
            try (InputStreamReader reader = new InputStreamReader(httpExchange.getRequestBody())) {
                char[] buffer = new char[256];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    body.append(buffer, 0, read);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONParser jsonParser = new JSONParser();
        try {
            return (JSONObject) jsonParser.parse(body.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
}
