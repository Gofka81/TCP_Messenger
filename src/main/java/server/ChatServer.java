package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import objects.Group;
import exceptions.ServerExceptions;

public class ChatServer {
    private static final String STATE_FILE = "server_state.ser";

    public void start() {
        try {
            ServerSocket server_socket = new ServerSocket(50002);
            ConnectionPool cp = new ConnectionPool();

            try {
                Map<String, Group> loadedGroups = cp.loadStateFromFile(STATE_FILE);
                cp.setGroupHandler(loadedGroups);
            } catch (ServerExceptions e) {
                System.err.println("Failed to load server state: " + e.getMessage());
            }

            // shutdown hook - runs to save state before JVM terminates
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Saving server state...");
                    cp.saveStateToFile(STATE_FILE, cp.getGroupHandler());
                } catch (ServerExceptions e) {
                    System.err.println("Failed to save server state: " + e.getMessage());
                }
            }));

            System.out.println("Server started ...");
            while (true){
                Socket socket = server_socket.accept();
                ChatServerHandler csh = new ChatServerHandler(socket, cp);
                cp.addConnection(csh);
                Thread th = new Thread(csh);
                th.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}