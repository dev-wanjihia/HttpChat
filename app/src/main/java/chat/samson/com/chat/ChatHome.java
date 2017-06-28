package chat.samson.com.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class ChatHome extends AppCompatActivity
{
    public static final String USER_KEY = "user";
    public static final String IS_SAVED = "is_saved";

    private  ViewPager contentViewPager;
    private TabLayout chatHomeTabs;

    private ChatHomePagerAdapter pagerAdapter;

    private BroadcastReceiver onMessageReceived;

    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_home);
        init();
        startService();
        registerReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.chat_home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_refresh_contacts:
                ((Contacts)pagerAdapter.getItem(1)).syncContacts();
                break;
            case R.id.menu_delete:
                if(contentViewPager.getCurrentItem() == 0) //Message Threads
                {
                    ((MessageThreads)pagerAdapter.getItem(0)).deleteSelected();
                }
                else if(contentViewPager.getCurrentItem() == 1)//Contacts
                {
                    ((Contacts)pagerAdapter.getItem(1)).deleteSelected();
                }
                break;
            case R.id.menu_home_my_profile:
                startActivity(new Intent(this, UserProfileActivity.class));
                break;
        }
        return true;
    }

    private void init()
    {
        contentViewPager = (ViewPager)findViewById(R.id.chat_home_pager);
        chatHomeTabs = (TabLayout)findViewById(R.id.chat_home_tabs);

        pagerAdapter = new ChatHomePagerAdapter(getSupportFragmentManager());

        contentViewPager.setAdapter(pagerAdapter);
        chatHomeTabs.setupWithViewPager(contentViewPager);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
    }

    private void registerReceiver()
    {
        onMessageReceived = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                /*NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                 notificationManager.cancel(MessageService.NOTIFICATION_ID); */
                ((MessageThreads)pagerAdapter.getItem(0)).refreshList();
            }
        };

        IntentFilter messageReceivedFilter = new IntentFilter();
        messageReceivedFilter.addAction("com.chat.ReceiveMessage");
        registerReceiver(onMessageReceived, messageReceivedFilter);
    }

    private void startService()
    {
        try
        {
            Thread t = new Thread()
            {
                @Override
                public void run() {
                    super.run();
                    startService(new Intent(getApplicationContext(), MessageService.class));
                }
            };

            t.run();
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(onMessageReceived);
    }


    private class ChatHomePagerAdapter extends FragmentStatePagerAdapter
    {
        Contacts contactsFragment;

        MessageThreads threadsFragment;

        public ChatHomePagerAdapter(FragmentManager fragmentManager)
        {
            super(fragmentManager);

            threadsFragment = new MessageThreads();
            contactsFragment = new Contacts();
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return position == 0 ? "MESSAGES" : "CONTACTS";
        }

        @Override
        public Fragment getItem(int position)
        {
            return position == 0 ? threadsFragment : contactsFragment;
        }

        @Override
        public int getCount()
        {
            return 2;
        }
    }

}
