package chat.samson.com.chat.data;

/**
 * Created by root on 3/1/17.
 */

public class MessageThread
{
    String lastMessage, userName;

    int unreadMessages;

    boolean userExists = false;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLastMessage()
    {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(int unreadMessages)
    {
        this.unreadMessages = unreadMessages;
    }

    public void setUserExists(boolean exists)
    {
        userExists = exists;
    }

    public boolean doesUserExists()
    {
        return userExists;
    }

}
