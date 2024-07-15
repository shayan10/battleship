public class Main {
    public static void main(String[] args) {
        Server serverConnection = new Server();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Server Resources");
            serverConnection.shutdown();
        }));
    }
}