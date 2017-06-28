package chat.samson.com.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

import chat.samson.com.chat.adapters.ThreadsAdapter;
import chat.samson.com.chat.data.DatabaseAdapter;
import chat.samson.com.chat.data.MessageThread;

public class MessageThreads extends Fragment implements View.OnClickListener
{
    private LinearLayout threadsContainer;

    private DatabaseAdapter dbAdapter;

    private ArrayList<MessageThread> messageThreads;

    private ThreadsAdapter threadsAdapter;

    private ListView threadsList;

    private BroadcastReceiver onMessageReceived;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        threadsContainer = (LinearLayout)(inflater.inflate(R.layout.threads_layout, null));
        return threadsContainer;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        init();
    }

    private void init()
    {
        dbAdapter = new DatabaseAdapter(getActivity());

        threadsList = (ListView)(threadsContainer.findViewById(R.id.list_threads));

        refreshList();

        onMessageReceived = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(MessageService.ACTION_MESSAGE);
        getActivity().registerReceiver(onMessageReceived, filter);
    }

    @Override
    public void onClick(View view)
    {
        int position = (Integer)view.getTag();

        Intent userChatIntent = new Intent();

        userChatIntent.setClass(getActivity(), UserChat.class);
        userChatIntent.putExtra(ChatHome.USER_KEY, messageThreads.get(position).getUserName());
        userChatIntent.putExtra(ChatHome.IS_SAVED, messageThreads.get(position).doesUserExists());

        startActivity(userChatIntent);
    }

    public void refreshList()
    {
        messageThreads = dbAdapter.getThreads();
        threadsAdapter = new ThreadsAdapter(messageThreads, getActivity(), this );
        threadsList.setAdapter(threadsAdapter);
        threadsList.setSelection(threadsList.getCount() - 1);
    }

    public void deleteSelected()
    {}


    @Override
    public void onResume()
    {
        //
        super.onResume();
    }

    @Override
    public void onDestroy()
    {
        getActivity().unregisterReceiver(onMessageReceived);
        super.onDestroy();
    }
}
