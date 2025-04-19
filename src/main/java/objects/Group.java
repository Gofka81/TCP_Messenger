package objects;

import exceptions.ServerExceptions;
import exceptions.ServerExceptions.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;

public class Group implements Recipient, Serializable{
    private final String groupName;
    private String groupOwner;
    private final Set<String> members = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private final String prefix;

    public Group(String group, String groupOwner) {
        this.groupName = group;
        this.groupOwner = groupOwner;
        this.prefix = "[Group: " + groupName +"]";
        members.add(groupOwner);
    }

    @Override
    public Set<String> getUserPool(Message msg) {
        if (msg.getUser() != null){
            return getTopicSubscribers(msg);
        }
        else{
            return getMembers();
        }
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    // ================================
    // Group Functional
    // ================================
    @Override
    public String getName(){
        return groupName;
    }

    public Set<String> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean isGroupOwner(String user){
        return groupOwner.equals(user);
    }

    public boolean isEmpty() { return members.isEmpty();}

    public synchronized void joinGroup(String user) throws UserAlreadyJoined{
        if (members.contains(user)) throw new UserAlreadyJoined();

        members.add(user);
    }

    public synchronized void leaveGroup(String user) throws UserIsNotJoined{
        if (!members.remove(user)) throw new UserIsNotJoined(groupName);

        if (isGroupOwner(user) && !isEmpty()) {
            groupOwner = members.iterator().next();
        }
    }

    // ================================
    // Topic Functional
    // ================================
    public synchronized void createTopic(String user, String topic) throws ServerExceptions {
        if (!members.contains(user)) throw new UserIsNotJoined(groupName);
        if (subscriptions.containsKey(topic)) throw new TopicAlreadyExists();
        subscriptions.put(topic, ConcurrentHashMap.newKeySet());
    }

    public synchronized void subscribeUser(String user, String topic) throws ServerExceptions {
        if (!members.contains(user)) throw new UserIsNotJoined(groupName);
        if (!subscriptions.containsKey(topic)) throw new DoNotExists(topic);
        subscriptions.get(topic).add(user);
    }

    public synchronized void unsubscribeUser(String user, String topic) throws ServerExceptions {
        if (!members.contains(user)) throw new UserIsNotJoined(groupName);
        if (!subscriptions.containsKey(topic)) throw new DoNotExists(topic);

        Set<String> subs = subscriptions.get(topic);
        subs.remove(user);
        if (subs.isEmpty()) {
            subscriptions.remove(topic);
        }
    }

    public Set<String> listTopics(){
        return Collections.unmodifiableSet(subscriptions.keySet());
    }

    public Set<String> listSubscribers(String topic){
        return Collections.unmodifiableSet(subscriptions.get(topic));
    }

    private synchronized Set<String> getTopicSubscribers(Message message){
        Set<String> triggeredTopics = new HashSet<>();
        Set<String> subscribers = new HashSet<>();
        String messageBody = message.getMessageBody().toLowerCase();

        for(String topic: listTopics()){
            if (messageBody.contains(topic)){
                triggeredTopics.add(topic);
            }
        }

        message.setTriggeredTopics(String.join(", ", triggeredTopics));
        System.out.println("Triggered topics: " + message.getTriggeredTopics());

        for (String topic: triggeredTopics){
            subscribers.addAll(listSubscribers(topic));
        }

        return subscribers;
    }

}
