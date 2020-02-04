import java.io.IOException;
import java.net.*;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;

public class SSH {
    public boolean isConnected = false;
    public boolean isConnecting = false;

    private SshClient client;
    private ClientSession session;
    private int port = -1;

    public boolean connect(String host, String username, String password) {
        isConnected = false;
        isConnecting = true;
        try {
            client = SshClient.setUpDefaultClient();
            client.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
            client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
            client.start();
            try {
                session = client.connect(username, host, 22).verify(5000).getSession();
                session.addPasswordIdentity(password);
                session.auth().verify(10000);
                SshdSocketAddress sshdSocketAddress = session.startDynamicPortForwarding(
                    new SshdSocketAddress("127.0.0.1", getFreePort())
                );
                Proxy proxy = new Proxy(
                    Proxy.Type.SOCKS,
                    new InetSocketAddress(sshdSocketAddress.getHostName(), sshdSocketAddress.getPort())
                );
                HttpURLConnection connection = (HttpURLConnection) new URL("https://www.googleapis.com/")
                .openConnection(proxy);
                connection.setConnectTimeout(2500);
                if (connection.getResponseCode() == 404) {
                    try {
                        this.port = sshdSocketAddress.getPort();
                        isConnected = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        isConnecting = false;
        return isConnected;
    }

    public void disconnect() {
        try {
            if (session != null) session.close();
        } catch (Exception ignored) {}
        try {
            if (client != null) client.stop();
        } catch (Exception ignored) {}
        isConnected = false;
        session = null;
        client = null;
    }

    public int getPort() {
        return port;
    }

    private static int getFreePort() {
        int port = -1;
        do {
            try {
                ServerSocket s = new ServerSocket(0);
                s.close();
                port = s.getLocalPort();
            } catch (Error | IOException e) {
                e.printStackTrace();
            }
        } while (port == -1);
        return port;
    }
}
