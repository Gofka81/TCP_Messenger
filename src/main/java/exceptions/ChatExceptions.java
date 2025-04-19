package exceptions;

public class ChatExceptions extends Exception {

    public ChatExceptions(String message){
        super(message);
    }

    public static class UserNotRegisteredException extends ChatExceptions {
        public UserNotRegisteredException() {
            super("Please register to send message.");
        }
    }

    public static class InvalidCommandException extends ChatExceptions {
        public InvalidCommandException(String message) {
            super(message);
        }
    }

    public static class UserAlreadyRegistered extends  ChatExceptions{
        public UserAlreadyRegistered() {
            super("User already registered. Please UNREGISTER first.");
        }
    }

    public static class HelpMeException extends ChatExceptions{
        public HelpMeException(String help){
            super(help);
        }
    }

    public static class CannotMessageYourselfException extends ChatExceptions {
        public CannotMessageYourselfException() {
            super("You cannot send a message to yourself.");
        }
    }
}
