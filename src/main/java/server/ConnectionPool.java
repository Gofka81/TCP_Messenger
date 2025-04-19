package server;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import exceptions.ServerExceptions;
import exceptions.ServerExceptions.*;

import objects.*;


public class ConnectionPool {
    private final Map<String, ChatServerHandler> userHandlers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ChatServerHandler> unregisteredHandlers = new CopyOnWriteArrayList<>();
    private final Map<String, Group> groupHandler = new ConcurrentHashMap<>();
    private static final Set<String> FORBIDDEN_NAMES = Set.of(
            "admin", "server", "null", "root", "system", "group", "user", "global"
    );

    public synchronized void  addConnection(ChatServerHandler handler) {
        unregisteredHandlers.add(handler);
    }

    public synchronized void addUser(String username, ChatServerHandler handler) throws ServerExceptions {
        if (isAvailableName(username)) {
            userHandlers.put(username, handler);
            unregisteredHandlers.remove(handler);
            return;
        }
        throw new UserNameIsTakenException();
    }

    public void removeUser(String username) {
        userHandlers.remove(username);
    }

    public synchronized void createGroup(String groupName, String userName) throws ServerExceptions {
        if (isAvailableName(groupName)) {
            Group group = new Group(groupName, userName);
            groupHandler.put(groupName, group);
            return;
        }
        throw new NameIsTakenException();
    }
    
    public String listUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append("Online Users:\n");
        Collection<ChatServerHandler> handlers = userHandlers.values();
        int index = 1;
        for (ChatServerHandler handler : handlers) {
            sb.append("[").append(index++).append("] ").append(handler.getUserName()).append("\n");
        }
        return sb.toString();
    }

    public String listGroups(String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append("List of Groups:\n");
        Collection<Group> groups = groupHandler.values();
        int index = 1;
        for (Group group : groups) {
            sb.append("[").append(index++).append("] ").append(group.getName());
            if (group.getMembers().contains(userName)) sb.append("\tJOINED");
            sb.append("\n");
        }
        return sb.toString();
    }

    public synchronized Group removeGroup(String groupName, String userName) throws ServerExceptions{
        Group group = groupHandler.get(groupName);
        if (group != null){
            if (group.isGroupOwner(userName)){
                return groupHandler.remove(groupName);
            }
            throw new NoGroupAdminRights();
        }
        throw new DoNotExists(groupName);
    }

    public synchronized void joinGroup(String groupName, String userName) throws ServerExceptions {
        Group group = groupHandler.get(groupName);
        if (group != null) {
            group.joinGroup(userName);
            return;
        }
        throw new DoNotExists(groupName);
    }

    public synchronized void leaveGroup(String groupName, String userName) throws ServerExceptions{
        Group group = groupHandler.get(groupName);
        if (group != null){
            group.leaveGroup(userName);
            if (group.isEmpty()){
                groupHandler.remove(groupName);
            }
            return;
        }
        throw new DoNotExists(groupName);
    }

    public synchronized void createTopic(String groupName, String user, String topic) throws ServerExceptions {
        Group group = groupHandler.get(groupName);
        if (group == null) throw new DoNotExists(groupName);

        topic = topic.toLowerCase();
        group.createTopic(user, topic);
        subscribeTopic(groupName, user, topic);
    }

    public synchronized void subscribeTopic(String groupName, String user, String topic) throws ServerExceptions{
        Group group = groupHandler.get(groupName);
        if (group == null) throw new DoNotExists(groupName);

        topic = topic.toLowerCase();
        group.subscribeUser(user, topic);
        userHandlers.get(user).getUser().addTopic(groupName, topic);
    }

    public synchronized void unsubscribeTopic(String groupName, String user, String topic) throws ServerExceptions{
        Group group = groupHandler.get(groupName);
        if (group == null) throw new DoNotExists(groupName);

        topic = topic.toLowerCase();
        group.unsubscribeUser(user, topic);
        userHandlers.get(user).getUser().removeTopic(groupName, topic);
    }

    public String listTopics(String groupName, String user) throws DoNotExists {
        Group group = groupHandler.get(groupName);
        if (group == null) throw new DoNotExists(groupName);

        StringBuilder sb = new StringBuilder();
        sb.append("List of Topics:\n");
        Set<String> userTopics = userHandlers.get(user).getUser().getTopics(groupName);
        Set<String> topics = group.listTopics();
        int index = 1;
        for (String topic : topics) {
            sb.append("[").append(index++).append("] ").append(topic);
            if (userTopics.contains(topic)) sb.append("\tSUBSCRIBED");
            sb.append("\n");
        }
        return sb.toString();
    }

    public String listMembers(String groupName) throws DoNotExists {
        Group group = groupHandler.get(groupName);
        if (group == null) throw new DoNotExists(groupName);

        StringBuilder sb = new StringBuilder();
        sb.append("Members:\n");
        Set<String> members = group.getMembers();
        int index = 1;
        for (String member : members) {
            sb.append("[").append(index++).append("] ").append(member);
            sb.append("\n");
        }
        return sb.toString();
    }

    public synchronized Set<String> scanMessageForNewTopic(Message message) throws ServerExceptions {
        Set<String> newTopics = new HashSet<>();
        String group = message.getTargetName();
        if (message.getUser() != null && groupHandler.containsKey(group)){
            Pattern pattern = Pattern.compile("#(\\w+)");
            Matcher matcher = pattern.matcher(message.getMessageBody().toLowerCase());

            Set<String> topics = groupHandler.get(group).listTopics();
            while (matcher.find()) {
                String topic = matcher.group(1);
                if (!topics.contains(topic)) {
                    createTopic(message.getTargetName(), message.getUser(),  topic);
                    newTopics.add(topic);
                }else{
                    subscribeTopic(message.getTargetName(), message.getUser(), topic);
                }
            }
        }
        return newTopics;
    }


    public synchronized void sendMessage(Message msg) throws ServerExceptions {
        String target = msg.getTargetName();
        Set<String> userPool;
        if (target.isEmpty()) {
            userPool = userHandlers.keySet();
            msg.setPrefix("[Global]");
        } else if (!isAvailableName(target)) {
            Recipient recipient = identifyRecipient(target);
            userPool = recipient.getUserPool(msg);
            System.out.println(userPool);
            if (userPool.isEmpty())
                throw new NoOneSawYourMessage();

            msg.setPrefix(recipient.getPrefix());
        } else {
            throw new MessageReceiverDoNotExists();
        }

        broadcast(userPool, msg);
    }
    
    public synchronized void saveStateToFile(String fileName, Map<String, Group> groups) throws ServerExceptions {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(groups);
            System.out.println("Server state saved to " + fileName);

        } catch (IOException e) {
            throw new ServerExceptions("Failed to save server state: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized Map<String, Group> loadStateFromFile(String fileName) throws ServerExceptions {
        File stateFile = new File(fileName);

        if (stateFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
                Object obj = ois.readObject();

                if (obj instanceof Map) {
                    System.out.println("Server state loaded from " + fileName);
                    return new ConcurrentHashMap<>((Map<String, Group>) obj);
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new ServerExceptions("Failed to load server state: " + e.getMessage());
            }
        }

        return new ConcurrentHashMap<>();
    }
    
    public Map<String, Group> getGroupHandler() {
        return groupHandler;
    }
    
    public void setGroupHandler(Map<String, Group> groupHandler) {
        this.groupHandler.clear();
        this.groupHandler.putAll(groupHandler);
    }

    protected synchronized void broadcast(Set<String> users, Message msg){
        for (String userName : users) {
            ChatServerHandler handler = userHandlers.get(userName);
            if (handler != null) {
                String sender = msg.getUser();
                String excludedUser = msg.getExcludedUser();
                if (!handler.getUserName().equals(sender) && !handler.getUserName().equals(excludedUser)){
                    handler.sendMessageToClient(msg);
                }
            }
        }
    }

    private synchronized boolean isAvailableName(String name) throws NameIsForbidden {
        if (FORBIDDEN_NAMES.contains(name.toLowerCase())){
            throw new NameIsForbidden();
        }
        return !(userHandlers.containsKey(name) || groupHandler.containsKey(name));
    }

    private synchronized Recipient identifyRecipient(String target) {
        if (userHandlers.containsKey(target)) {
            return userHandlers.get(target).getUser();
        } else {
            return groupHandler.get(target);
        }
    }
}