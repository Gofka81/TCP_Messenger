package objects;

import java.io.Serializable;

public enum Command implements Serializable {

    // Client commands
    REGISTER, UNREGISTER, SEND, HELP, USERS,
    CREATE, REMOVE, JOIN, LEAVE, GROUPS, MEMBERS, //Group manipulation
    SUBSCRIBE, UNSUBSCRIBE, TOPIC, TOPICS, //Topic manipulation

    // General client-server command
    MESSAGE,

    // Server callback
    ERROR, SUCCESS;

    public static Command fromString(String str) {
        try {
            return Command.valueOf(str);
        } catch (IllegalArgumentException e) {
            return MESSAGE;
        }
    }

    public static final String HELP_TEXT =
            "Available Commands:\n\n" +

            "General Commands:\n" +
            "  REGISTER <user_name>              - Register as a new user with a unique name.\n" +
            "  UNREGISTER                        - Unregister and disconnect from the server.\n" +
            "  USERS                             - List all online users in the server.\n" +
            "  HELP                              - Display this help message.\n\n" +

            "Group Commands:\n" +
            "  GROUPS                           - List all available groups in the server.\n" +
            "  MEMBERS <group_name>             - List all members of the group.\n" +
            "  CREATE <group_name>              - Create a new group.\n" +
            "  REMOVE <group_name>              - Remove a group you own.\n" +
            "  JOIN <group_name>                - Join an existing group.\n" +
            "  LEAVE <group_name>               - Leave a group you're a member of.\n\n" +

            "Messaging:\n" +
            "  SEND <recipient> <message>       - Send a message to a user or a group.\n" +
            "                                     Use SEND global <message> to broadcast globally.\n\n" +

            "Topic Commands:\n" +
            "  TOPIC <group_name> <topic>       - Create/register a new topic inside a group.\n" +
            "  TOPICS <group_name>              - List all available topics in a group.\n" +
            "  SUBSCRIBE <group_name> <topic>   - Subscribe to a topic in a specific group.\n" +
            "  UNSUBSCRIBE <group_name> <topic> - Unsubscribe from a topic in a specific group.\n\n" +

            "Hashtag Shortcut:\n" +
            "  You can also trigger topic creation using hashtags in messages.\n" +
            "  Example: \"Loving #AI and #Gaming these days\" will auto-create \"AI\" and \"Gaming\" topics.\n\n" +

            "Note:\n" +
            "  - Topics are group-limited means that topic subscriptions only apply within the group.\n" +
            "  - Messages will be delivered to users in a group only if they are subscribed to a topic mentioned in the message.\n";
}
