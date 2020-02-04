public class App {

    public static void main(String[] args) {
        SSH ssh = new SSH();
        System.out.println(ssh.connect("113.172.209.151", "admin", "admin") + "");
    }
}
