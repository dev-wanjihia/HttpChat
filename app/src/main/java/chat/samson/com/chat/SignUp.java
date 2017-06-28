package chat.samson.com.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.inputmethodservice.KeyboardView;
import android.net.Network;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.regex.Pattern;

public class SignUp  extends AppCompatActivity implements DialogInterface.OnClickListener, View.OnFocusChangeListener, TextWatcher
{
    private Resources resources;

    private NetworkThread networkThread;

    private OnClickListener btnClickHandler;

    private LinearLayout btnSignUp;

    private CheckBox checkEmail, checkUserName, checkSignUp1, checkSignUp2;

    private EditText editUserName, editEmail, current;

    private String userName, email;

    private ProgressDialog progressDialog;

    private AlertDialog.Builder infoDialog;

    private Bundle netData;

    @Override
    protected void onCreate( Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sign_up);
        init();

        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);

        if(prefs.contains(NetworkThread.ACTION_KEY))
        {
            restoreProgress();
        }
    }

    private void init()
    {
        resources = this.getResources();

        infoDialog = new AlertDialog.Builder(this);

        networkThread = new NetworkThread();

        progressDialog = new ProgressDialog(this);

        btnClickHandler = new OnClickListener() {
            @Override
            public void onClick(View view)
            { resolveClick(view); }
        };

        btnSignUp = (LinearLayout)findViewById(R.id.btn_sign_up_container);

        editUserName = (EditText)findViewById(R.id.edit_user_name);
        editEmail = (EditText)findViewById(R.id.edit_email);

        editEmail.setOnFocusChangeListener(this);
        editUserName.setOnFocusChangeListener(this);
        editUserName.addTextChangedListener(this);
        editEmail.addTextChangedListener(this);

        checkEmail = (CheckBox)findViewById(R.id.check_email);
        checkUserName = (CheckBox)findViewById(R.id.check_user_name);
        checkSignUp1 = (CheckBox)findViewById(R.id.check_sign_up);
        checkSignUp2 =(CheckBox)findViewById(R.id.check_sign_up_2);

        btnSignUp.setOnClickListener(btnClickHandler);

        infoDialog.setNeutralButton("OKAY", this);
    }

    private void resolveClick(View view)
    {
        switch(view.getId())
        {
            case R.id.btn_sign_up_container:
                signUp();
                break;
        }
    }

    private void init_ver()
    {
        Intent verificationIntent = new Intent();

        verificationIntent.putExtra(NetworkThread.USERNAME_KEY, userName);
        verificationIntent.putExtra(NetworkThread.EMAIL_KEY, email);
        verificationIntent.setClass(getBaseContext(), Verify.class);
        startActivity(verificationIntent);

        getPreferences(Context.MODE_PRIVATE).edit().
                putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_VERIFY).commit();

        finish();
    }

    private void signUp()
    {
        userName = editUserName.getText().toString();
        email = editEmail.getText().toString();

        if(TextUtils.isEmpty(userName))
        {
            checkUserName.setChecked(false);
            editUserName.requestFocus();
            return;
        }

        if(TextUtils.isEmpty(email))
        {
            checkEmail.setEnabled(false);
            editEmail.requestFocus();
            return;
        }

        progressDialog.setMessage(resources.getString(R.string.signing_up));
        progressDialog.show();

        netData = new Bundle();

        netData.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_SIGN_UP);
        netData.putString(NetworkThread.EMAIL_KEY, email);
        netData.putString(NetworkThread.USERNAME_KEY, userName);

        networkThread = new NetworkThread();
        networkThread.execute(netData);

        progressDialog.hide();
        progressDialog.cancel();
        progressDialog.dismiss();

        try
        {
            JSONObject result = networkThread.get();

            if(result == null)
            {
                infoDialog.setMessage(resources.getString(R.string.connection_error) + result);
                infoDialog.create().show();
                return;
            }

            int status = result.getInt("result");

            progressDialog.hide();

            switch(status)
            {
                case 0:
                    init_ver();
                    break;

                case -911:
                    userNameTaken();
                    break;

                case -910:
                    emailInUse();
                    break;

                case -909:
                    serverFailed();
                    break;

                case -907:
                    alreadySignedUp();
                    break;
            }
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            progressDialog.hide();
        }
    }

    private void userNameTaken()
    {
        checkUserName.setChecked(false);
        editUserName.requestFocus();
        Toast.makeText(this, resources.getString(R.string.username_taken, userName), Toast.LENGTH_SHORT).show();
    }

    private void emailInUse()
    {
        checkEmail.setChecked(false);
        editEmail.requestFocus();
        Toast.makeText(this, resources.getString(R.string.email_taken), Toast.LENGTH_SHORT).show();
    }

    private void serverFailed()
    {
        infoDialog.setMessage(resources.getString(R.string.server_failed));
        infoDialog.show();
    }

    private void alreadySignedUp()
    {
        DialogInterface.OnClickListener dialogButtonsHandler = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch(i)
                {
                    case DialogInterface.BUTTON_POSITIVE:

                        netData.remove(NetworkThread.ACTION_KEY);
                        netData.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_RESEND_KEY);

                        progressDialog.setMessage(resources.getString(R.string.resending_key, email));
                        progressDialog.show();

                        networkThread = new NetworkThread();
                        networkThread.execute(netData);

                        try
                        {

                            JSONObject result = networkThread.get();

                            if (result == null)
                            {
                                progressDialog.cancel();
                                infoDialog.setMessage(resources.getString(R.string.connection_error));
                                infoDialog.create().show();
                                return;
                            }

                            if(result.getBoolean("sent"))
                            {
                                progressDialog.hide();
                                infoDialog.setMessage(resources.getString(R.string.ver_code_sent, email));
                                AlertDialog d = infoDialog.create();
                                d.show();

                                init_ver();

                                d.cancel();
                            }
                        }
                        catch(Exception exception)
                        {
                            exception.printStackTrace();
                        }
                        finally
                        {
                            progressDialog.dismiss();
                            progressDialog.cancel();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        infoDialog.setMessage(resources.getString(R.string.sign_up_with_other_email));
                        infoDialog.show();
                        break;
                }
            }
        };

        infoDialog.setMessage(resources.getString(R.string.signed_up));
        infoDialog.setPositiveButton("Proceed", dialogButtonsHandler);
        infoDialog.setNegativeButton("Not Me", dialogButtonsHandler);

        infoDialog.setNeutralButton(null, null);

        infoDialog.show();

        infoDialog.setPositiveButton(null, null);
        infoDialog.setNegativeButton(null,  null);

        infoDialog.setNeutralButton("OKAY", this);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int buttonId)
    {
        if(buttonId == DialogInterface.BUTTON_NEUTRAL)
        {
            dialogInterface.dismiss();
            dialogInterface.cancel();
        }
    }

    @Override
    public void onFocusChange(View view, boolean focus)
    {

        switch(view.getId())
        {
            case R.id.edit_email:

                if(focus)
                    current = editEmail;

                String email = editEmail.getText().toString();
                checkEmail.setChecked(Patterns.EMAIL_ADDRESS.matcher(email).matches());

                break;
            case R.id.edit_user_name:

                if(focus)
                    current = editUserName;

                Pattern userNamePattern = Pattern.compile("[\\w\\d@#_\\+\\*\\-]{3,}");
                checkUserName.setChecked(userNamePattern.matcher(editUserName.getText().toString()).matches());

                break;
        }

        checkSignUp1.setChecked(checkUserName.isChecked() && checkEmail.isChecked());
        checkSignUp2.setChecked(checkUserName.isChecked() && checkEmail.isChecked());
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
        if(current != null)
        {
            if (current.getId() == R.id.edit_email)
            {
                checkEmail.setChecked(Patterns.EMAIL_ADDRESS.matcher(current.getText().toString()).matches());
            }
            else
            {
                Pattern userNamePattern = Pattern.compile("[\\w\\d@#_\\+\\*\\-]{3,}");
                checkUserName.setChecked(userNamePattern.matcher(editUserName.getText().toString()).matches());
            }
        }

        checkSignUp1.setChecked(checkEmail.isChecked() && checkUserName.isChecked());
        checkSignUp2.setChecked(checkEmail.isChecked() && checkUserName.isChecked());
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        saveProgress();
    }

    private void saveProgress()
    {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(NetworkThread.USERNAME_KEY, editUserName.getText().toString());
        editor.putString(NetworkThread.EMAIL_KEY, editEmail.getText().toString());

        editor.commit();

    }

    private void restoreProgress()
    {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);

        email = preferences.getString(NetworkThread.EMAIL_KEY, "");
        userName = preferences.getString(NetworkThread.USERNAME_KEY, "");

        editEmail.setText(email);
        editUserName.setText(userName);

        //position is the position within the application.
        int position = preferences.getInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_SIGN_UP);


        if(position == NetworkThread.ACTION_VERIFY)
        {
            init_ver();

        }
    }

}
