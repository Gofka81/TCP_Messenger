package objects;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class User implements Recipient {
    private final String name;
    private final String prefix = "[Private Message]";
    private final Map<String, Set<String>> topics = new ConcurrentHashMap<>();


    public User(String name) {
        this.name = name;
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getUserPool(Message msg) {
        return Set.of(name);
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return "User{name='" + name + "'}";
    }

    public Set<String> getTopics(String group) {
        if (topics.get(group) !=  null){
            return Collections.unmodifiableSet(topics.get(group));
        }
        else{
            return Collections.emptySet();
        }
    }

    public void addTopic(String group, String topic){
        topics.putIfAbsent(group, new HashSet<>());
        topics.get(group).add(topic);
    }

    public void removeTopic(String group, String topic){
        topics.get(group).remove(topic);
    }
}
