package chat.samson.com.chat;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import chat.samson.com.chat.adapters.MessagesAdapter;
import chat.samson.com.chat.adapters.UserAdapter;
import chat.samson.com.chat.data.DatabaseAdapter;
import chat.samson.com.chat.data.Message;
import chat.samson.com.chat.data.User;
import chat.samson.com.chat.data.UserProfile;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserChat extends AppCompatActivity implements View.OnLongClickListener, View.OnClickListener {
    public static final String ACTION_FORWARD = "com.chat.Forward";
    public static Context USER_CHAT_CONTEXT;

    //TODO add the contact for unsaved contacts
    private DatabaseAdapter dbAdapter;

    private NetworkThread networkThread;
    private UserProfile userProfile;
    private MediaHandler mediaHandler;
    private User user;

    private Bundle netData;
    private EditText messageInput;
    private ListView messagesList;
    private CircleImageView profileImageView;

    private Dialog actionsDialog;

    private BroadcastReceiver onMessageReceived;
    private int msgInContext;
    private boolean isSaved;

    private ArrayList<Message> userMessages;
    private MessagesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_layout);

        init();

        onMessageReceived = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                //notificationManager.cancel(MessageService.NOTIFICATION_ID);
                Log.w("UserChat", "messageReceived");
                newMessage();
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MessageService.ACTION_MESSAGE_SINGLE + user.getUsername().toLowerCase());
        registerReceiver(onMessageReceived, intentFilter);

        if (getIntent().getAction() != null && getIntent().getAction().equals(ACTION_FORWARD)) {
            String message = getIntent().getExtras().getString(NetworkThread.MESSAGE_KEY);
            forward(message);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        actionsDialog = new Dialog(this);

        actionsDialog.setContentView(R.layout.message_action);


        actionsDialog.findViewById(R.id.delete).setOnClickListener(this);
        actionsDialog.findViewById(R.id.foward).setOnClickListener(this);

        msgInContext = Integer.parseInt(((String[]) view.getTag())[1]);

        actionsDialog.show();
        return true;
    }

    @Override
    public void onClick(View view) {
        Message message = (Message) messagesList.getAdapter().getItem(msgInContext);

        switch (view.getId()) {
            case R.id.delete:
                dbAdapter.deleteMessage(message.getMessageId());
                refreshMessages();
                actionsDialog.cancel();
                break;

            case R.id.foward:
                final Intent forwardIntent = new Intent();

                forwardIntent.setAction("com.chat.Forward");
                forwardIntent.putExtra(NetworkThread.MESSAGE_KEY, message.getMessage());
                forwardIntent.setClass(this, UserChat.class);

                Dialog contactsDialog = new Dialog(this);
                final ListView listContacts = new ListView(this);

                final View.OnClickListener contactItemClicked = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        User contactClicked = (User) listContacts.getAdapter().getItem((int) view.getTag());
                        forwardIntent.putExtra(ChatHome.USER_KEY, contactClicked.getUsername());
                        actionsDialog.cancel();
                        startActivity(forwardIntent);
                        finish();
                    }
                };

                listContacts.setAdapter(new UserAdapter(dbAdapter.getUsers(), this, contactItemClicked));

                contactsDialog.setContentView(listContacts);
                contactsDialog.show();
                break;

        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(onMessageReceived);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_menu, menu);
        menu.getItem(0).setVisible(isSaved);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save_contact:
                addToContacts();
                break;
            case R.id.menu_clear:
                clearMessages();
                break;
            case R.id.menu_forward:
                forwardMessage();
                break;
            case R.id.menu_search:
                searchForMessage();
                break;
        }
        return true;
    }

    private void init() {
        USER_CHAT_CONTEXT = getApplicationContext();

        dbAdapter = new DatabaseAdapter(this);
        networkThread = new NetworkThread();
        mediaHandler = new MediaHandler(this);

        Bundle extras = getIntent().getExtras();
        isSaved = extras.getBoolean(ChatHome.IS_SAVED, true);

        userProfile = dbAdapter.getUserProfile();
        user = dbAdapter.getUser(extras.getString(ChatHome.USER_KEY));
        userMessages = dbAdapter.getUserMessages(user.getUsername());

        TextView textUserName = (TextView) findViewById(R.id.txt_user_name);
        messageInput = (EditText) findViewById(R.id.edit_message);
        messagesList = (ListView) findViewById(R.id.list_messages);

        profileImageView = (CircleImageView) findViewById(R.id.contact_profile_picture);
        File pictureFile = new File(mediaHandler.getProfilePicturesDir(), user.getUsername() + ".png");
        final Bitmap profileBmp = pictureFile.exists() ? BitmapFactory.decodeFile(pictureFile.getAbsolutePath()) :
                BitmapFactory.decodeResource(getResources(), R.mipmap.ic__user);
        profileImageView.setImageBitmap(profileBmp
        );
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Dialog imageDialog = new Dialog(view.getContext(), android.support.design.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert);

                imageDialog.setContentView(R.layout.image_dialog);

                TextView titleView = (TextView) imageDialog.findViewById(R.id.text_title);
                ImageView imageView = (ImageView) imageDialog.findViewById(R.id.viewing_image);

                imageView.setImageBitmap(profileBmp);
                titleView.setText(user.getUsername());

                imageDialog.setCancelable(true);
                imageDialog.setCanceledOnTouchOutside(true);

                imageDialog.show();

            }
        });

        netData = new Bundle();

        refreshMessages();

        messagesList.setSelection(messagesList.getCount() - 1);
        textUserName.setText(user.getUsername());
        dbAdapter.setMessagesToRead(userMessages);

        messagesList.setSelection(messagesList.getCount() - 1);
    }

    public void sendMessage(View view) {
        String messageContent = messageInput.getText().toString();

        netData = new Bundle();

        if (TextUtils.isEmpty(messageContent))
            Toast.makeText(this, "Message is empty", Toast.LENGTH_SHORT).show();

        netData.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_SEND_MESSAGE);
        netData.putString(NetworkThread.USERNAME_KEY, userProfile.getUserName());
        netData.putString(NetworkThread.TOKEN_KEY, userProfile.getToken());
        netData.putString(NetworkThread.USERNAME_TO_KEY, user.getUsername());
        netData.putString(NetworkThread.TIME_KEY, getTime());
        netData.putString(NetworkThread.MESSAGE_KEY, messageContent);

        networkThread = new NetworkThread();
        networkThread.execute(netData);

        try {
            JSONObject results = networkThread.get();

            if (results == null) {
                Toast.makeText(this, getResources().getString(R.string.message_not_sent), Toast.LENGTH_SHORT).show();
                Log.w("Chat Engine :", "Connection Error.");
                return;
            }

            Message message = new Message();

            message.setUserName(user.getUsername());
            message.setMessage(netData.getString(NetworkThread.MESSAGE_KEY));
            message.setTime(netData.getString(NetworkThread.TIME_KEY));
            message.setType(Message.TYPE_SENT);

            if (results.getBoolean("sent")) {
                message.setStatus(Message.STATUS_SENT);
            }

            messageInput.setText("");
            dbAdapter.addMessage(message.getContentValues());

            refreshMessages();
            messagesList.setSelection(messagesList.getCount() - 1);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        networkThread = new NetworkThread();
    }

    private String getTime() {
        String time = "";

        Calendar calendar = Calendar.getInstance();

        time += calendar.get(Calendar.YEAR);

        int[] fields = new int[]{calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)};

        for (int field : fields) {
            if (field < 10)
                time += "0" + field;
            else
                time += field;
        }

        return time;
    }

    private void newMessage() {
        dbAdapter.setMessagesToRead(dbAdapter.getUserMessages(user.getUsername()));
        refreshMessages();
        messagesList.setSelection(messagesList.getCount() - 1);
    }

    private void addToContacts() {
        //TODO Implement add to contacts
    }

    private void clearMessages() {
        dbAdapter.clearMessages(user.getUsername());
        refreshMessages();
    }

    private void searchForMessage() {
        //
        //
    }

    private void forwardMessage() {
        //
        //

    }

    private void forward(String message) {
        //
        messageInput.setText(message);
    }

    private void refreshMessages() {
        userMessages = dbAdapter.getUserMessages(user.getUsername());
        adapter = new MessagesAdapter(userMessages);
        adapter.setLongClickListener(this);
        messagesList.setAdapter(adapter);
    }

}
