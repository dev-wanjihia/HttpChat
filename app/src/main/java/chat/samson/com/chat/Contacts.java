package chat.samson.com.chat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.support.v4.app.Fragment;

import chat.samson.com.chat.adapters.UserAdapter;
import chat.samson.com.chat.data.DatabaseAdapter;
import chat.samson.com.chat.data.User;
import chat.samson.com.chat.data.UserProfile;

public class Contacts extends Fragment implements View.OnClickListener
{
    public static final int MODE_NORMAL = 0, MODE_FORWARD = 1;

    private int mode = MODE_NORMAL;

    UserProfile userProfile;

    NetworkThread networkThread;

    DatabaseAdapter dbAdapter;

    ListView contactsList;

    LinearLayout contactsListContainer;

    UserAdapter contactsAdapter;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        //
        init();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        contactsListContainer = (LinearLayout)inflater.inflate(R.layout.contacts_layout, null);
        return contactsListContainer;
    }

    private void init()
    {
        dbAdapter = new DatabaseAdapter(getActivity());

        networkThread = new NetworkThread();

        userProfile = dbAdapter.getUserProfile();

        contactsList = (ListView)contactsListContainer.findViewById(R.id.contacts_list);

        refreshContacts();
    }

    private void refreshContacts()
    {
        contactsAdapter = new UserAdapter(dbAdapter.getUsers(), getActivity(), this);
        contactsList.setAdapter(contactsAdapter);
        contactsList.setSelection(contactsList.getCount() - 1);
    }

    @Override
    public void onClick(View view)
    {
        int position = (Integer)view.getTag();

        Intent userChatIntent = new Intent();
        userChatIntent.setClass(getActivity(), UserChat.class);
        userChatIntent.putExtra(ChatHome.USER_KEY, ((User)contactsAdapter.getItem(position)).getUsername());

        switch(mode)
        {
            case MODE_NORMAL:
                startActivity(userChatIntent);
                break;
            case MODE_FORWARD:
                userChatIntent.setAction(UserChat.ACTION_FORWARD);
                startActivity(userChatIntent);
                break;
        }
    }

    public void syncContacts()
    {
        final ProgressDialog dialog = new ProgressDialog(getActivity());

        SyncContacts syncContactsTask = new SyncContacts()
        {

            @Override
            public void onStart()
            {
                dialog.setMessage("Contacts sync in progress...");
                dialog.show();
            }

            @Override
            public void onComplete(int status)
            {
                dialog.hide();
                refreshContacts();
            }
        };

        syncContactsTask.start(getActivity());
    }

    public void deleteSelected()
    {
    }
}
