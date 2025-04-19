package client;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import exceptions.ChatExceptions;
import objects.Command;
import objects.Message;
import exceptions.ChatExceptions.*;

import static objects.Command.HELP_TEXT;

public class ChatClient {
    private ObjectInputStream inStream;
    private boolean isRegistered = false;
    private String userName = "";
    public void startClient() {
        try (
                Socket socket = new Socket("localhost", 50002);
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                Scanner scanner = new Scanner(System.in)
        ) {
            inStream = new ObjectInputStream(socket.getInputStream());
            Thread thread = new Thread(this::listenToServer);
            thread.setDaemon(true);
            thread.start();

            System.out.println("A client starts...");
            while (true) {
                try {
                    String strMsg = scanner.nextLine();

                    Message instanceMessage = createMessage(strMsg);
                    outStream.writeObject(instanceMessage);
                    if (instanceMessage.getCommand() == Command.UNREGISTER) {
                        break;
                    }
                } catch (HelpMeException help) {
                    System.out.println(successMessage(help.getMessage()));
                }
                catch (ChatExceptions e) {
                    System.out.println(errorMessage("Error: " + e.getMessage()));
                }
            }
            System.out.println("Program successfully finished.");
        } catch (IOException e) {
            System.out.println(errorMessage("Connection error: " + e.getMessage()));
        }
    }

    private void listenToServer() {
        try {
            while (true){
                Message msg = (Message) inStream.readObject();
                StringBuilder content = new StringBuilder(msg.getPrefix()).append(" ");
                String message;
                if (msg.getTriggeredTopics() != null) content.append("[Topics: ").append(msg.getTriggeredTopics()).append("]");
                if (msg.getUser() != null) content.append(" ").append(msg.getUser()).append(": ");
                content.append(msg.getMessageBody());
                message = content.toString().trim();

                if (msg.getCommand() == Command.SUCCESS) {
                    if (msg.getMessageBody().contains("Registration successful!")){
                        isRegistered = true;
                    }
                    message = successMessage(message);
                } else if (msg.getCommand() == Command.ERROR) {
                    if (msg.getMessageBody().contains("User name is already taken.")){
                        userName = null;
                    }
                    message = errorMessage(message);
                }

                System.out.println(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(errorMessage("Lost connection to server."));
        }
    }

    private String errorMessage(String message){
        return "\u001B[31m" + message + "\u001B[0m";
    }

    private String successMessage(String message){
        return "\u001B[32m" + message + "\u001B[0m";
    }

    private Message createMessage(String strMsg) throws ChatExceptions {
        Message instanceMessage = processClientMessage(strMsg);
        if (!isRegistered && instanceMessage.getCommand() != Command.REGISTER){
            throw new UserNotRegisteredException();
        }
        if (isRegistered && instanceMessage.getCommand() == Command.REGISTER){
            throw new UserAlreadyRegistered();
        }

        if (instanceMessage.getUser() == null){
            instanceMessage.setUser(userName);
        }
        else{
            userName = instanceMessage.getUser();
        }
        return instanceMessage;
    }

    public Message processClientMessage(String input) throws ChatExceptions {
        String[] parts = input.trim().split("\\s+", 3);
        Command command = Command.fromString(parts[0]);
        Message message = null;

        switch (command) {
            case REGISTER:
                if (parts.length == 2 && !parts[1].isEmpty()) {
                    String user = parts[1];
                    message = Message.forRegisterClient(command, user);
                    break;
                }
                throw new InvalidCommandException("Please follow the format: REGISTER <user_name>");
            case UNREGISTER:
            case USERS:
            case GROUPS:
                if (parts.length == 1) {
                    message = Message.forMessageClient(command, "");
                    break;
                }
                throw new InvalidCommandException("Command "+ command +" is not using any parameters.");
            case CREATE:
            case REMOVE:
            case JOIN:
            case LEAVE:
            case TOPICS:
            case MEMBERS:
                if (parts.length == 2 && !parts[1].isEmpty()) {
                    String group = parts[1];
                    message = Message.forTargetClient(command, group, "");
                    break;
                }
                throw new InvalidCommandException("Invalid " + command + " command. Usage: " + command + " <group_name>");

            case TOPIC:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                if (parts.length == 3 && !parts[1].isEmpty() && !parts[2].isEmpty()) {
                    String group = parts[1];
                    String topic = parts[2];
                    message = Message.forTargetClient(command, group, topic);
                    break;
                }
                throw new InvalidCommandException("Invalid " + command + " command. Usage: " + command + " <group_name> <topic_name>");

            case SEND:
                if (parts.length >= 3 && !parts[1].isEmpty() && !parts[2].isEmpty()) {
                    String recipient = parts[1];
                    String messageContent = parts[2];

                    if (recipient.equalsIgnoreCase("global")){
                        message = Message.forMessageClient(command, messageContent);
                    }else {
                        if (recipient.equals(userName)) {
                            throw new CannotMessageYourselfException();
                        }
                        message = Message.forTargetClient(command, recipient, messageContent);
                    }
                    break;
                }
                throw new InvalidCommandException("Invalid SEND command. Recipient name can't contain spaces. Usage: SEND <recipient_name> <message>");

            case HELP:
                if (parts.length == 1) {
                    throw new HelpMeException(HELP_TEXT);
                }
                throw new InvalidCommandException("Command "+ command +" is not using any parameters.");
            default:
                throw new InvalidCommandException("No such command. Use HELP to list all available commands.");
        }

        return message;
    }

}