package server;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;

import exceptions.ServerExceptions;
import objects.*;

public class ChatServerHandler implements Runnable{
    private final Socket socket;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private final ConnectionPool pool;
    private User user;
    private boolean active = true;

    public ChatServerHandler(Socket socket, ConnectionPool pool){
        this.socket = socket;
        this.pool = pool;
        try {
            this.inStream = new ObjectInputStream(socket.getInputStream());
            this.outStream = new
                    ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        try {
            while (active) {
                Message message = (Message) inStream.readObject();
                processMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Connection lost for user: " + (user != null ? user.getName() : "Unknown"));
        }finally {
            processLeave();
        }
    }

    private void processLeave() {
        try {
            if (user != null) {
                pool.removeUser(user.getName());
                System.out.println("User " + user.getName() + " has been removed from the pool.");

                Message announce = Message.forMessageServer(
                        Command.MESSAGE,
                        "User " + user.getName() + " has left."
                );
                pool.sendMessage(announce);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException | ServerExceptions ex) {
            System.err.println("Error during cleanup: " + ex.getMessage());
        }
    }

    public synchronized void sendMessageToClient(Message msg){
        try {
            outStream.writeObject(msg);
        } catch (IOException e) {
            // TODO Come up with adequate exception handling
            throw new RuntimeException(e);
        }
    }
    public User getUser() {
        return this.user;
    }

    public String getUserName(){
        if (user != null){
            return user.getName();
        }
        return null;
    }

    private void processMessage(Message message) {
        Command command = message.getCommand();
        Message callback = null;
        System.out.println(message);
        try{
            switch (command) {
                case REGISTER: {
                    pool.addUser(message.getUser(), this);
                    callback = Message.forMessageServer(Command.SUCCESS, "Registration successful!");
                    user = new User(message.getUser());
                    Message announce = Message.forMessageServer(
                            Command.MESSAGE,
                            "User " + user.getName() + " has joined."
                    );
                    announce.setExcludedUser(message.getUser());
                    pool.sendMessage(announce);
                    break;
                }
                case UNREGISTER: {
                    active = false;
                    break;
                }
                case CREATE: {
                    pool.createGroup(message.getTargetName(), message.getUser());
                    callback = Message.forMessageServer(Command.SUCCESS, "Group " + message.getTargetName() + " successfully created.");
                    break;
                }
                case REMOVE: {
                    Group deletedGroup = pool.removeGroup(message.getTargetName(), message.getUser());
                    Message groupAnnounce = Message.forMessageServer(Command.MESSAGE, deletedGroup.getName(),
                            "Group " + deletedGroup.getName() + " has been removed.");
                    groupAnnounce.setPrefix(deletedGroup.getPrefix());
                    // Since we removed target, we broadcast it to group members
                    pool.broadcast(deletedGroup.getMembers(), groupAnnounce);
                    break;
                }
                case JOIN: {
                    String group = message.getTargetName();
                    pool.joinGroup(group, message.getUser());
                    Message groupAnnounce = Message.forMessageServer(Command.MESSAGE, group,
                            "User " + message.getUser() + " joined group.");
                    groupAnnounce.setExcludedUser(message.getUser());
                    pool.sendMessage(groupAnnounce);
                    callback = Message.forMessageServer(Command.SUCCESS, "You successfully joined " + message.getMessageBody());
                    break;
                }
                case LEAVE: {
                    String groupName = message.getTargetName();
                    Message groupAnnounce = Message.forMessageServer(Command.MESSAGE, groupName,
                            "User " + message.getUser() + " left group.");
                    groupAnnounce.setExcludedUser(message.getUser());
                    pool.sendMessage(groupAnnounce);
                    pool.leaveGroup(groupName, message.getUser());
                    callback = Message.forMessageServer(Command.SUCCESS, "You left " + message.getMessageBody());
                    break;
                }
                case USERS:{
                    String usersList = pool.listUsers();
                    callback = Message.forMessageServer(Command.MESSAGE, message.getTargetName(), usersList);
                    break;
                }
                case GROUPS:{
                    String groupList = pool.listGroups(message.getUser());
                    callback = Message.forMessageServer(Command.MESSAGE, message.getTargetName(), groupList);
                    break;
                }
                case MEMBERS:{
                    String membersList = pool.listMembers(message.getTargetName());
                    callback = Message.forMessageServer(Command.MESSAGE, message.getTargetName(), membersList);
                    break;
                }
                case TOPICS:{
                    String topicsList = pool.listTopics(message.getTargetName(), message.getUser());
                    callback = Message.forMessageServer(Command.MESSAGE, message.getTargetName(), topicsList);
                    break;
                }
                case TOPIC: {
                    String topic = message.getMessageBody();
                    pool.createTopic(message.getTargetName(), message.getUser(), topic);
                    callback = Message.forMessageServer(Command.SUCCESS, "Topic #" + topic + " successfully created.");
                    break;
                }
                case SUBSCRIBE: {
                    String topic = message.getMessageBody();
                    pool.subscribeTopic(message.getTargetName(), message.getUser(), topic);
                    callback = Message.forMessageServer(Command.SUCCESS, "You successfully joined #" + topic + " topic.");
                    break;
                }
                case UNSUBSCRIBE: {
                    String topic = message.getMessageBody();
                    pool.unsubscribeTopic(message.getTargetName(), message.getUser(), topic);
                    callback = Message.forMessageServer(Command.SUCCESS, "You unsubscribed #" + topic + " topic.");
                    break;
                }
                case SEND:{
                    Set<String> newTopics = pool.scanMessageForNewTopic(message);

                    if (!newTopics.isEmpty()){
                        callback = Message.forMessageServer(Command.SUCCESS, "Topics: " + String.join(", ", newTopics) + " successfully created.");
                    }
                    pool.sendMessage(message);
                }
            }
        } catch (ServerExceptions exception) {
            callback = Message.forMessageServer(Command.ERROR, exception.getMessage());
        }
        finally {
            if (callback != null) sendMessageToClient(callback);
        }
    }
}