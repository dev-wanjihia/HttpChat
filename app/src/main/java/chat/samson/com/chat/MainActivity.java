package chat.samson.com.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import chat.samson.com.chat.data.DatabaseAdapter;

public class MainActivity extends Activity {
    DatabaseAdapter dbAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        dbAdapter = new DatabaseAdapter(getBaseContext());

        if (dbAdapter.isLoggedIn())
        {
            startActivity(new Intent(getApplicationContext(), ChatHome.class));
            finish();
        }
        else
        {
            startActivity(new Intent(getApplicationContext(), SignUp.class));
            finish();
        }
        finish();
    }
}