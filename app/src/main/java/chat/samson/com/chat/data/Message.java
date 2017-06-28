package chat.samson.com.chat.data;

import android.content.ContentValues;

/**
 * Wrapper class for Messages telephone, userName, status, type
 */

public class Message
{
    private static DatabaseAdapter dbAdapter;

    private String userName;
    private String time;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    int status, type, msgid = -1;

    public static int STATUS_PENDING = 0, STATUS_SENT = 1, STATUS_READ = 2, STATUS_UNREAD = 3;
    public static int TYPE_SENT = 0, TYPE_RECEIVED = 1;

    private String message;

    public void setMessageId(int id)
    {
        this.msgid = id;
    }

    public int getMessageId()
    {
        return msgid;
    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public ContentValues getContentValues()
    {
        ContentValues values = new ContentValues();

        if(msgid != -1)
            values.put("message_id", msgid);

        values.put("message", message);
        values.put("type", type);
        values.put("status", status);
        values.put("user_name", userName.toLowerCase());
        values.put("msg_time", time);

        return values;
    }
}
