package objects;

import java.util.Set;

public interface Recipient {
    String getName();

    Set<String> getUserPool(Message msg);

    String getPrefix();
}
