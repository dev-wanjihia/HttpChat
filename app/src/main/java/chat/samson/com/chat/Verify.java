package chat.samson.com.chat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.View.OnClickListener;

import org.json.JSONObject;

import chat.samson.com.chat.data.DatabaseAdapter;
import chat.samson.com.chat.data.UserProfile;

public class Verify extends AppCompatActivity implements OnClickListener, View.OnFocusChangeListener, TextWatcher
{
    private DatabaseAdapter dbAdapter;

    private NetworkThread networkThread;

    private Bundle networkData;

    private String userName, verificationCode, email;

    private EditText editUserName, editCode;

    private LinearLayout btnVerification;

    CheckBox checkCode, checkUserName, checkVerify, checkVerify1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify);

        userName = getIntent().getExtras().getString(NetworkThread.USERNAME_KEY);
        email = getIntent().getExtras().getString(NetworkThread.EMAIL_KEY);

        init();
    }

    private void init()
    {
        dbAdapter = new DatabaseAdapter(this);

        networkThread = new NetworkThread();

        networkData = new Bundle();

        btnVerification = (LinearLayout)findViewById(R.id.btn_verify_container);

        editCode = (EditText)findViewById(R.id.edit_verification_code);
        editUserName = (EditText)findViewById(R.id.edit_user_name);

        checkCode = (CheckBox)findViewById(R.id.check_code);
        checkUserName =(CheckBox)findViewById(R.id.check_user_name);

        checkVerify = (CheckBox)findViewById(R.id.check_verify);
        checkVerify1 = (CheckBox)findViewById(R.id.check_verify_1);

        editUserName.setText(userName);
        editUserName.setEnabled(false);
        editCode.addTextChangedListener(this);
        checkUserName.setChecked(true);

        btnVerification.setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        resolveClick(view);
    }

    private void resolveClick(View clickedView)
    {
        switch (clickedView.getId())
        {
            case R.id.btn_verify_container:
                verify();
                break;
        }
    }

    private void verify()
    {
        verificationCode = editCode.getText().toString();

        if(verificationCode.length() != 6)
        {
            checkCode.setChecked(false);
            checkVerify.setChecked(false);
            checkVerify1.setChecked(false);

            return;
        }

        networkData.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_VERIFY);
        networkData.putString(NetworkThread.USERNAME_KEY, userName);
        networkData.putString(NetworkThread.VERIFICATION_KEY, verificationCode);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.verifying));
        progressDialog.show();

        networkThread = new NetworkThread();

        networkThread.execute(networkData);

        try
        {
            JSONObject result = networkThread.get();
            progressDialog.cancel();

            if(result == null)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getResources().getString(R.string.connection_error));
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return;
            }

            boolean verified = result.getBoolean("verified");


            if(verified)
            {
                String token = result.getString("code");

                UserProfile userProfile = new UserProfile();

                userProfile.setUserName(userName);
                userProfile.setEmail(email);
                userProfile.setToken(token);

                dbAdapter.insertProfile(userProfile);

                Intent chatHomeIntent = new Intent();

                chatHomeIntent.setClass(getBaseContext(), ChatHome.class);
                startActivity(chatHomeIntent);
                finish();
            }
            else
            {
                int failureCode = Integer.parseInt(result.getString("code"));

                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                switch(failureCode) {
                    case -908:
                        builder.setMessage(getResources().getString(R.string.wrong_ver_code));
                        checkCode.setChecked(false);
                        break;
                    case -907:
                        builder.setMessage(getResources().getString(R.string.server_failed));
                        checkVerify.setChecked(false);
                        checkVerify1.setChecked(false);
                        break;
                }

                builder.create().show();
            }

            progressDialog.hide();
        }
        catch (Exception exception)
        {
        }
        finally
        {
            progressDialog.cancel();
        }
    }

    @Override
    public void onFocusChange(View view, boolean b)
    {

    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void afterTextChanged(Editable editable)
    {
        String verificationCode = editCode.getText().toString();
        boolean checked = verificationCode.matches("\\d{6}");

        checkCode.setChecked(checked);

        checkVerify.setChecked(checked);
        checkVerify1.setChecked(checked);
    }
}
