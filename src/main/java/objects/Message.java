package objects;

import java.io.Serializable;

public class Message implements Serializable {

    private final Command command;
    private String messageBody;
    private String user;
    private String targetName = "";
    private String prefix = "";
    private String excludedUser = null;

    private String triggeredTopics;


    private Message(Command command) {
        this.command = command;
    }

    public static Message forRegisterClient(Command command, String user) {
        Message msg = new Message(command);
        msg.user = user;
        return msg;
    }

    public static Message forMessageClient(Command command, String message) {
        Message msg = new Message(command);
        msg.messageBody = message;
        return msg;
    }

    public static Message forMessageServer(Command status, String message) {
        Message msg = new Message(status);
        msg.messageBody = message;
        return msg;
    }

    public static Message forMessageServer(Command status, String targetName, String message) {
        Message msg = forMessageServer(status, message);
        msg.targetName = targetName;
        return msg;
    }

    public static Message forTargetClient(Command command, String targetName, String message) {
        Message msg = new Message(command);
        msg.messageBody = message;
        msg.targetName = targetName;
        return msg;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String userName) {
        this.user = userName;
    }

    public Command getCommand() {
        return command;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getExcludedUser() {
        return excludedUser;
    }

    public void setExcludedUser(String excludedUser) {
        this.excludedUser = excludedUser;
    }


    public String getTriggeredTopics() {
        return triggeredTopics;
    }

    public void setTriggeredTopics(String triggeredTopics) {
        this.triggeredTopics = triggeredTopics;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(user).append(": ").append(command).append(" ");
        if (!targetName.isBlank()){
            sb.append(targetName).append(" ");
        }
        if (!targetName.isEmpty()){
            sb.append(messageBody);
        }
        return sb.toString();
    }
}