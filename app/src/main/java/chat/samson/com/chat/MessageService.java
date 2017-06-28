package chat.samson.com.chat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;

import javax.security.auth.login.LoginException;

import chat.samson.com.chat.data.DatabaseAdapter;
import chat.samson.com.chat.data.Message;
import chat.samson.com.chat.data.User;
import chat.samson.com.chat.data.UserProfile;

public class MessageService extends Service implements Runnable
{
    //TODO Change the refresh time based on online activity.
    //TODO Add the default notification ringtone fotr the message.
    //TODO add read notification from server

    Handler handler;
    public static int NOTIFICATION_ID = 100;

    public static boolean RUNNING = false;

    public static final String ACTION_MESSAGE_SINGLE = "com.chat.ReceiveMessage.SINGLE",

                               ACTION_MESSAGE = "com.chat.ReceiveMessage",
                                ACTION_MESSAGE_SERVICE = "com.chat.Service";

    public static final long REFRESH_LONG = 5000, REFRESH_SHORT = 1000;

    UserProfile userProfile;

    DatabaseAdapter dbAdapter;

    BroadcastReceiver refreshChange;

    private long refreshTimer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(getClass().getSimpleName(), "Low Memory");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Thread serviceThread = new Thread()
        {
            @Override
            public void run()
            {
                init();
                super.run();
            }
        };

        serviceThread.run();

        return START_STICKY;
    }

    private void init()
    {
        handler = new Handler();

        dbAdapter = new DatabaseAdapter(getBaseContext());

        userProfile = dbAdapter.getUserProfile();

        if(dbAdapter.isLoggedIn())
        {
            refreshChange = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent)
                {

                }
            };

            run();

            RUNNING = true;
            Log.w("Chat Engine ", "MessageService Started successfully");
        }
        else
        {
            stopSelf();
        }
    }


    private void checkForMessages()
    {
        long prevTime = System.currentTimeMillis(), timeTaken;

        try
        {
            NetworkThread networkThread = new NetworkThread();
            Bundle data = new Bundle();

            data.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_CHECK_MESSAGE);
            data.putString(NetworkThread.USERNAME_KEY, userProfile.getUserName());
            data.putString(NetworkThread.TOKEN_KEY, userProfile.getToken());

            networkThread.execute(data);

            JSONObject result = networkThread.get();

            if(result == null)
            {
                return;
            }

            int numMessages = result.getInt("messages_found");

            if(numMessages  > 0)
            {
                getMessages(numMessages);
            }
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            timeTaken = System.currentTimeMillis() - prevTime;

            handler.postAtTime(this, timeTaken < 900 ? 1000 : timeTaken);
        }
    }

    private void getMessages(int numMessages)
    {
        try
        {
            NetworkThread networkThread = new NetworkThread();

            Bundle data = new Bundle();

            data.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_GET_MESSAGES);
            data.putString(NetworkThread.TOKEN_KEY, userProfile.getToken());
            data.putString(NetworkThread.USERNAME_KEY, userProfile.getUserName());

            networkThread.execute(data);

            JSONObject messages = networkThread.get();

            if(messages == null)
            {
                Log.w("Chat Engine :", "Connection Error");
                return;
            }

            JSONArray msgArray = messages.getJSONArray("messages");
            Intent broadCast = new Intent();

            if(numMessages == 1)
            {
                String action = ACTION_MESSAGE_SINGLE + msgArray.getJSONObject(0).getString("username").toLowerCase();
                broadCast.setAction(action);
            }


            while(numMessages > 0)
            {
                JSONObject message = msgArray.getJSONObject(numMessages - 1);

                Message temp = new Message();

                temp.setMessage(message.getString("message"));
                temp.setTime(message.getString("msg_time"));
                temp.setUserName(message.getString("username"));
                temp.setType(Message.TYPE_RECEIVED);
                temp.setStatus(Message.STATUS_UNREAD);

                //The message has been sent from a user who is not our contacts.
                if(dbAdapter.getUser(temp.getUserName()).getUsername().isEmpty())
                {
                    NetworkThread getEmail = new NetworkThread();
                    Bundle reqData = new Bundle();
                    User user = new User();

                    reqData.putString(NetworkThread.USERNAME_KEY, userProfile.getUserName());
                    reqData.putString(NetworkThread.TOKEN_KEY, userProfile.getToken());
                    reqData.putString(NetworkThread.USERNAME_TO_KEY, temp.getUserName());

                    getEmail.execute(reqData);
                    JSONObject jsonEmail = getEmail.get();
                    String email = jsonEmail.getString("email");

                    user.setUsername(temp.getUserName());
                    user.setEmail(email);
                    user.setInContacts(false);

                    dbAdapter.insertUser(user);
                }

                dbAdapter.addMessage(temp.getContentValues());

                numMessages --;
            }

            notifyMessagesFound();

            getBaseContext().sendBroadcast(broadCast);

            Intent messageReceived = new Intent();
            messageReceived.setAction(ACTION_MESSAGE);
            getBaseContext().sendBroadcast(messageReceived);
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
            Log.e("ChatEngine :", exception.toString());
        }
    }

    @Override
    public void run()
    {

        checkForMessages();
    }

    private void notifyMessagesFound()
    {
        //Builder that will be used to create the notification.
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext())
                .setSmallIcon(R.mipmap.ic_message_notification)
                .setContentText("You have " + dbAdapter.getUnreadMessages() + " new messages.")
                .setContentTitle("New Messages")
                .setVibrate(new long[]{12,12})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));


        //This is the stack of the intent for when we click back
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getBaseContext());
        stackBuilder.addParentStack(ChatHome.class);

        Intent resultIntent = new Intent();
        resultIntent.setClass(getBaseContext(), ChatHome.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void setRefreshTimer(long refreshTimer)
    {
        this.refreshTimer = refreshTimer;
    }

    @Override
    public void onDestroy() {
        RUNNING = false;
        Log.w("Chat Engine:", "Message Service Stopped");
        Intent serviceFilter = new Intent();
        serviceFilter.setAction(ACTION_MESSAGE_SERVICE);
        sendBroadcast(serviceFilter);
        super.onDestroy();
    }

    @Override
    public boolean stopService(Intent name) {
        Log.w("Chat Engine:", "Message Service Stopped");
        return super.stopService(name);
    }
}
