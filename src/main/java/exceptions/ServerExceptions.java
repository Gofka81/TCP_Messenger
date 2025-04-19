package exceptions;

public class ServerExceptions extends Exception {

    public ServerExceptions(String message){
        super(message);
    }

    public static class UserNameIsTakenException extends ServerExceptions {
        public UserNameIsTakenException() {
            super("User name is already taken.");
        }
    }

    public static class NameIsTakenException extends ServerExceptions {
        public NameIsTakenException() {
            super("This name is already taken.");
        }
    }

    public static class MessageReceiverDoNotExists extends ServerExceptions{
        public MessageReceiverDoNotExists(){
            super("Message receiver do not exists or left. Check who you send message.");
        }
    }

    public static class UserAlreadyJoined extends ServerExceptions {
        public UserAlreadyJoined() {
            super("User already in this group/topic.");
        }
    }

    public static class DoNotExists extends ServerExceptions {
        public DoNotExists(String name){
            super("The " + name + " do not exist.");
        }
    }

    public static class NoGroupAdminRights extends ServerExceptions {
        public NoGroupAdminRights(){
            super("You dont have rights to do so.");
        }
    }

    public static class UserIsNotJoined extends ServerExceptions {
        public UserIsNotJoined(String name){
            super("You are not in "+name+".");
        }
    }

    public static class TopicAlreadyExists extends ServerExceptions {
        public TopicAlreadyExists(){
            super("Such topic already exists.");
        }
    }

    public static class NameIsForbidden extends  ServerExceptions{
        public NameIsForbidden(){ super("Such name is forbidden.");}
    }

    public static class NoOneSawYourMessage extends ServerExceptions{
        public NoOneSawYourMessage(){ super("Your message was not sent. No topics were triggered in the group.");}
    }
}
