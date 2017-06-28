package chat.samson.com.chat.data;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;
import android.text.TextUtils;

import java.util.ArrayList;


public class DatabaseAdapter
{
    /**
     * Store messages that we have sent, received and those that are pending to be sent.
     * messages -> message, user_name, status, type, message_id, time
     **/

    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;
    private String dbName = "chats.db";
    Context context;

    public DatabaseAdapter(Context context)
    {
        this.context = context;
        dbHelper = new DatabaseHelper(context, dbName);
    }

    public void insertProfile(UserProfile profile)
    {
        database = dbHelper.getWritableDatabase();

        if(!isLoggedIn())
            database.insert("profile", null, profile.getValues());
        else
        {
            database.update("profile", profile.getValues(), "", null);

            if(!profile.getUserName().equalsIgnoreCase(getUserProfile().getUserName()))
            {
                //The user has changed.
                database.execSQL("delete from messages;");
            }
        }
    }

    public boolean isLoggedIn()
    {
        database = dbHelper.getReadableDatabase();

        Cursor result = database.rawQuery("select count(user_name) as num_users from profile;", null);
        result.moveToFirst();
        boolean res = result.getInt(result.getColumnIndex("num_users")) == 1;
        result.close();

        return res;
    }

    public UserProfile getUserProfile()
    {
        database = dbHelper.getReadableDatabase();

        UserProfile user = new UserProfile();

        Cursor userData = database.query(true, "profile", null, null, null, null, null, null, null);

        if(userData.moveToFirst())
        {
            user.setProfilePicture(userData.getString(userData.getColumnIndex("profile_picture")));
            user.setUserName(userData.getString(userData.getColumnIndex("user_name")));
            user.setEmail(userData.getString(userData.getColumnIndex("email")));
            user.setToken(userData.getString(userData.getColumnIndex("auth_token")));
        }

        userData.close();
        return user;
    }

    public ArrayList<Message> getUserMessages(String userName)
    {
        database = dbHelper.getReadableDatabase();
        userName = userName.toLowerCase();

        // Get all messages sent and received from a user with userName

        ArrayList<Message> userMessages = new ArrayList<>();
        String query = "select * from messages where user_name = ?;";
        Cursor queryResults = database.rawQuery(query, new String[]{ userName });

        if(queryResults.moveToFirst())
            do
            {
                Message temp = new Message();

                temp.setTime(queryResults.getString(queryResults.getColumnIndex("msg_time")));
                temp.setMessage(queryResults.getString(queryResults.getColumnIndex("message")));
                temp.setUserName(queryResults.getString(queryResults.getColumnIndex("user_name")));
                temp.setType(queryResults.getInt(queryResults.getColumnIndex(("type"))));
                temp.setStatus(queryResults.getInt(queryResults.getColumnIndex("status")));
                temp.setMessageId(queryResults.getInt(queryResults.getColumnIndex("message_id")));

                userMessages.add(temp);
            }
            while(queryResults.moveToNext());

        queryResults.close();

        return userMessages;
    }

    public ArrayList<User> getUsers()
    {
        database = dbHelper.getReadableDatabase();

        ArrayList<User> results = new ArrayList<>();

        Cursor data = database.rawQuery("select * from users where not user_name  = ? ;",
                                        new String[]{getUserProfile().getUserName()});

        if(data.moveToFirst())
        {
            do
            {
                User temp = new User();

                temp.setEmail(data.getString(data.getColumnIndex("email")));
                temp.setUsername(data.getString(data.getColumnIndex("user_name")));
                temp.setProfilePicture(data.getString(data.getColumnIndex("profile_picture")));

                results.add(temp);
            }
            while(data.moveToNext());
        }
        data.close();

        

        return results;
    }

    public User getUser(String userName)
    {
        database = dbHelper.getReadableDatabase();

        String userSQL = "select user_name, email, in_contacts from users where user_name = ? ;";
        Cursor userCursor = database.rawQuery(userSQL, new String[]{userName.toLowerCase()});

        if(userCursor.moveToFirst())
        {
            User user = new User();

            user.setUsername(userCursor.getString(userCursor.getColumnIndex("user_name")));
            user.setEmail(userCursor.getString(userCursor.getColumnIndex("email")));
            user.setInContacts(userCursor.getInt(userCursor.getColumnIndex("in_contacts")) == 1);

            userCursor.close();

            return user;
        }

        userCursor.close();
        return new User();
    }

    public void insertUser(User user)
    {
        database = dbHelper.getWritableDatabase();

        try
        {
            if(TextUtils.isEmpty(getUser(user.username).getUsername()))
                database.insert("users", null, user.genValues());
            else
                database.update("users",  user.genValues(), " user_name = ?", new String[]{user.getUsername()});
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    public void addMessage(ContentValues values)
    {
        database = dbHelper.getWritableDatabase();

        database.insert("messages", null, values);
    }

    public ArrayList<MessageThread> getThreads()
    {
        database = dbHelper.getReadableDatabase();
        ArrayList<MessageThread> messageThreads = new ArrayList<>();

        String countUnreadSQL = "select message_id from messages where user_name = ? and status = " + Message.STATUS_UNREAD + ";",
                lastUserMessageSQL = "select message as last_message from messages where user_name = ? order by msg_time desc limit 1;",
                sendersSQL = "select user_name from messages group by user_name;",
                userExistsSQL = "select in_contacts from users where user_name = ?;";

        Cursor sendersCursor = database.rawQuery(sendersSQL, null);

        if(sendersCursor.moveToFirst())
        {
            do
            {
                String currUser = sendersCursor.getString(sendersCursor.getColumnIndex("user_name")).toLowerCase();

                Cursor lastMessageCursor = database.rawQuery(lastUserMessageSQL, new String[]{currUser}),
                        countUnreadCursor = database.rawQuery(countUnreadSQL, new String[]{currUser}),
                        userExistsCursor = database.rawQuery(userExistsSQL, new String[]{currUser});

                lastMessageCursor.moveToFirst();
                countUnreadCursor.moveToFirst();
                userExistsCursor.moveToFirst();

                MessageThread tempThread = new MessageThread();

                tempThread.setUserName(currUser);
                tempThread.setLastMessage(lastMessageCursor.getString(0));
                tempThread.setUserExists(userExistsCursor.getInt(0) == 1);
                tempThread.setUnreadMessages(countUnreadCursor.getCount());

                messageThreads.add(tempThread);

                lastMessageCursor.close();
                countUnreadCursor.close();
                userExistsCursor.close();
            }
            while(sendersCursor.moveToNext());
        }

        sendersCursor.close();

        return messageThreads;
    }

    public int getUnreadMessages()
    {
        database = dbHelper.getReadableDatabase();

        String numUnread = "select count(message_id) as num_unread from messages where type = ? and status = ?;";
        Cursor unreadMessagesCursor = database.rawQuery(numUnread,
                new String[]{   Integer.toString(Message.TYPE_RECEIVED),
                                Integer.toString(Message.STATUS_UNREAD)});

        unreadMessagesCursor.moveToFirst();
        int unreadMessages = unreadMessagesCursor.getInt(unreadMessagesCursor.getColumnIndex("num_unread"));
        unreadMessagesCursor.close();

        
        return unreadMessages;
    }

    public void setMessagesToRead(ArrayList<Message> readMessages)
    {
        database = dbHelper.getWritableDatabase();

        for(Message readMessage : readMessages)
        {
            readMessage.setStatus(Message.STATUS_READ);
            database.update("messages", readMessage.getContentValues(),
                            "message_id = ?", new String[]{Integer.toString(readMessage.getMessageId())});
        }
        
    }

    public void setUserMessagesToRead(String userName)
    {
        database = dbHelper.getWritableDatabase();

        String readMessagesSQL = "update messages set status = ? where user_name = ? where status = ? ";
        database.execSQL(readMessagesSQL, new String[]{Integer.toString(Message.STATUS_READ), userName.toLowerCase(), Integer.toString(Message.STATUS_UNREAD)});
    }

    public void deleteMessage(int messageID)
    {
        database = dbHelper.getWritableDatabase();
        database.delete("messages", "message_id = ?", new String[]{messageID + ""});
        
    }

    public void addToContacts(User user)
    {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations
                .add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA1, user.getEmail())
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, user.getUsername())
                .build());
        try
        {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }
    }

    public void clearMessages(String userName)
    {
        database = dbHelper.getReadableDatabase();
        String deleteUserMessagesSQL = "delete from messages where user_name = '" + userName.toLowerCase() + "'";
        database.execSQL(deleteUserMessagesSQL);
    }


    private class DatabaseHelper extends SQLiteOpenHelper
    {
        /*Messages message_id -> primary key to the message, user_name -> sender of the message, message -> actual message
        status -> (read, unread, sent, pending), type -> (sent, received), msg_time -> time when message was sent.

        user_profile user_name -> user_name of the user, email, auth_token

        users/contacts -> in_contacts (0, 1), email -> email of the user, profile_picture -> name of the profile picture of the user
        will be appended to the other full path for reading.
        user_name -> name of the user to be used during communication
        */



        String createTableMessages = "create table messages (message_id integer primary key autoincrement, " +
                                     "user_name text, message text, status integer, type integer, msg_time text);",
                createTableProfile = "create table profile (user_name text, email text, auth_token text, profile_picture text);",
                createTableUsers = "create table users (in_contacts integer, email varchar(20), profile_picture text," +
                        " user_name varchar(20) primary key unique not null);";

        DatabaseHelper(Context context, String dbName)
        {
            super(context, dbName, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(createTableMessages);
            db.execSQL(createTableProfile);
            db.execSQL(createTableUsers);

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
        {
        }
    }
}
