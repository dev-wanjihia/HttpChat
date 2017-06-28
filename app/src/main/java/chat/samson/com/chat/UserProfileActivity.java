package chat.samson.com.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

import chat.samson.com.chat.data.DatabaseAdapter;
import chat.samson.com.chat.data.UserProfile;

/*******************************************
 * Created by Wanjihia                     *
 * time :  *
 *******************************************/

public class UserProfileActivity extends AppCompatActivity {
    private static final int GET_PICTURE = 100, CROP_PICTURE = 200;
    private static final String CHANGE_PROFILE_PICTURE = "com.wanjihia.chat.ChangeImage";
    private NetworkThread networkThread;
    private DatabaseAdapter dbAdapter;
    private UserProfile userProfile;
    private TextView textEmail;
    private ImageView imageProfilePicture;
    private Bundle netDataBundle;
    private Toolbar toolbar;
    private File profilePicture;
    private MediaHandler mediaHandler;
    private BroadcastReceiver changePictureReceiver;
    private Intent broadCastIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile_layout);
        init();
        changePictureReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                imageProfilePicture.setImageBitmap(BitmapFactory.decodeFile(profilePicture.getAbsolutePath()));
            }
        };
        registerReceiver(changePictureReceiver, new IntentFilter(CHANGE_PROFILE_PICTURE));

        broadCastIntent = new Intent(CHANGE_PROFILE_PICTURE);
    }

    private void init() {
        mediaHandler = new MediaHandler(this);
        dbAdapter = new DatabaseAdapter(this);

        userProfile = dbAdapter.getUserProfile();
        netDataBundle = new Bundle();

        toolbar = (Toolbar) findViewById(R.id.user_profile_toolbar);
        textEmail = (TextView) findViewById(R.id.user_profile_email);
        imageProfilePicture = (ImageView) findViewById(R.id.user_profile_ppic);
        profilePicture = new File(mediaHandler.getPersonalDataPath(),  MediaHandler.PROFILE_PICTURE_FILE_NAME);

        textEmail.setText(userProfile.getEmail());
        String userName = Character.toUpperCase(userProfile.getUserName().charAt(0))
                + userProfile.getUserName().substring(1);

        toolbar.setTitle(userName);

        setSupportActionBar(toolbar);

        netDataBundle.putString(NetworkThread.USERNAME_KEY, userProfile.getUserName());
        netDataBundle.putString(NetworkThread.TOKEN_KEY, userProfile.getToken());
        netDataBundle.putString(NetworkThread.EMAIL_KEY, userProfile.getEmail().toLowerCase());

        //If the profile picture exists and it matches the one available
        if (profilePicture.exists())
            imageProfilePicture.setImageBitmap(BitmapFactory.decodeFile(profilePicture.getAbsolutePath()));
        else
            imageProfilePicture.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic__user));

        harmonizeLocalWithRemote();
    }

    private void harmonizeLocalWithRemote()
    {
        Runnable netRunnable = new Runnable() {
            @Override
            public void run() {
                if(!isValid())
                {
                    getPhotoFromServer();
                }
            }
        };
        Thread netThread = new Thread(netRunnable);
        netThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_user_profile_edit_ppic:
                updateProfilePicture();
                break;
            case R.id.menu_user_profile_remove_ppic:
                removeProfilePicture();
                break;
            case R.id.menu_user_profile_delete_account:
                deleteAccount();
                break;
        }
        return true;
    }

    private void updateProfilePicture() {
        netDataBundle.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_UPLOAD_PROFILE);

        Intent pictureSelectIntent = new Intent();

        pictureSelectIntent.setType("image/*");
        pictureSelectIntent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(pictureSelectIntent, "Select Photo"), GET_PICTURE);
    }

    private void removeProfilePicture() {
        netDataBundle.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_UPLOAD_PROFILE);
        netDataBundle.putBoolean(NetworkThread.REMOVE_PROFILE_PIC_KEY, true);

        networkThread = new NetworkThread();
        networkThread.execute(netDataBundle);

        try {
            JSONObject results = networkThread.get();

            if (results != null && results.getBoolean("upload_status") ) {
                imageProfilePicture.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic__user));
                profilePicture.delete();
                userProfile.setProfilePicture("");
                dbAdapter.insertProfile(userProfile);
            } else if (profilePicture.exists()) {
                imageProfilePicture.setImageBitmap(BitmapFactory.decodeFile(profilePicture.getAbsolutePath()));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            netDataBundle.remove(NetworkThread.REMOVE_PROFILE_PIC_KEY);
        }
    }

    private void deleteAccount() {

    }

    private void getPhotoFromServer() {

        //Gets the link to the photo in the server and then saves this photo in the profile_picture
        //then sets the image resource to the image that is now going to be stored in the profile_picture file.
        //downloads the image

        netDataBundle.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_GET_PROFILE_PIC);
        netDataBundle.putStringArray(NetworkThread.EMAIL_ADDRESSES_KEY, new String[]{userProfile.getEmail()});

        networkThread = new NetworkThread();

        networkThread.execute(netDataBundle);

        try {
            JSONObject results = networkThread.get();

            if (results != null && results.getInt("response") == 0) {
                MediaHandler profileDownloader = new MediaHandler(this);
                Bundle profileParams = new Bundle();

                profileParams.putInt(MediaHandler.WHICH_KEY, MediaHandler.MY_PROFILE_PIC);
                profileParams.putString(MediaHandler.DOWNLOAD_BASE_URL_KEY, results.getString("base_url"));
                profileParams.putString(MediaHandler.FILE_LINK_URL, results.getString(userProfile.getEmail()));

                Intent changeProfile = new Intent();
                changeProfile.setAction(CHANGE_PROFILE_PICTURE);
                sendBroadcast(changeProfile);

                profileDownloader.execute(profileParams);

                try {
                    Boolean[] downloadStatus = profileDownloader.get();

                    if (downloadStatus != null && downloadStatus[0]) {
                        //The profile picture has been downloaded successfully and now all we need to do is set it up in out imageView
                        requestRefresh();
                    }
                }catch (Exception exception)
                {
                    //We could not connect and now we need to cry.
                    exception.printStackTrace();
                    errorHandler(getResources().getString(R.string.connection_error));
                }
            }
        } catch (Exception exception) {
            errorHandler(getResources().getString(R.string.connection_error));
            exception.printStackTrace();
        }
    }

    private boolean isValid() {
        //Return true if the image should not be gotten from server

        String remoteProfilePicture = "", localProfilePicture = userProfile.getProfilePicture();

        NetworkThread profileChecker = new NetworkThread();
        Bundle profileBundle = new Bundle();

        profileBundle.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_GET_PROFILE_PIC);
        profileBundle.putStringArray(NetworkThread.EMAIL_ADDRESSES_KEY, new String[]{userProfile.getEmail()});
        profileBundle.putString(NetworkThread.USERNAME_KEY, userProfile.getUserName());
        profileBundle.putString(NetworkThread.TOKEN_KEY, userProfile.getToken());

        JSONObject profileResults;

        try {
            profileChecker.execute(profileBundle);

            profileResults = profileChecker.get();

            if (profileResults != null) {
                //Checks if the user has been authorized otherwise show the error snackbar
                if(profileResults.getInt("response") == 0)
                    remoteProfilePicture = profileResults.getString(userProfile.getEmail());
                else
                    errorHandler(getResources().getString(R.string.unauthorized_err));

            } else {
                errorHandler(getResources().getString(R.string.connection_error));
                return true;
            }
        } catch (Exception exception) {
            errorHandler(getResources().getString(R.string.connection_error));
            return true;
        }

        return (localProfilePicture != null) && (localProfilePicture.equalsIgnoreCase(remoteProfilePicture));
    }

    private void saveProfilePicture(Bitmap profileBitmap) {

        /**
         *
         * Receives the profile picture as a bitmap and then
         * the new one is saved in a temporary file called temp_profile.png
         * if temp_file does not exist then create it if not possible
         * then we assume that there is not enough storage space and end there
         * if the temporary profile picture was created successfully then we are going
         * or does exist then we are going to overwrite it.
         * and save it there.
         * put parameters for profile picture upload username and token
         * request for upload.
         *
         * if the connection was successful and the upload status was okay then delete
         * the current profile picture then update the new profile picture path in the
         * database for later
         * otherwise show that an error occurred.
         *
         */

        try {
            String picturePath = profilePicture.getAbsolutePath();
            File profilePicture = new File(mediaHandler.getPersonalDataPath(), "temp_profile.png");


            if (!profilePicture.exists() && !profilePicture.createNewFile()) {
                errorHandler(getResources().getString(R.string.insufficient_storage));
                return;
            }


            profileBitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(profilePicture));

            netDataBundle.putString(NetworkThread.PROFILE_PIC_PATH_KEY, profilePicture.getAbsolutePath());

            networkThread = new NetworkThread();
            networkThread.execute(netDataBundle);

            JSONObject uploadResults = networkThread.get();

            if (uploadResults != null && uploadResults.getBoolean("upload_status")) {
                File delFile = new File(picturePath);

                delFile.delete();

                profilePicture.renameTo(new File(picturePath));
                userProfile.setProfilePicture(uploadResults.getString("profile_picture"));
                dbAdapter.insertProfile(userProfile);

            } else {
                errorHandler(getResources().getString(R.string.connection_error));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case GET_PICTURE:
                    Intent cropIntent = new Intent();

                    cropIntent.setAction("com.android.camera.action.CROP");
                    cropIntent.setData(data.getData());
                    cropIntent.putExtra("crop", true);

                    cropIntent.putExtra("scaleX", 300);
                    cropIntent.putExtra("scaleY", 300);

                    cropIntent.putExtra("outputX", 300);
                    cropIntent.putExtra("outputY", 300);
                    cropIntent.putExtra("return-data", true);

                    startActivityForResult(cropIntent, CROP_PICTURE);
                    break;

                case CROP_PICTURE:
                    Bitmap croppedImage = data.getExtras().getParcelable("data");
                    imageProfilePicture.setImageBitmap(croppedImage);
                    saveProfilePicture(croppedImage);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void errorHandler(String errorMessage)
    {
        final Snackbar errorBar = Snackbar.make(imageProfilePicture.getRootView(), errorMessage, Snackbar.LENGTH_INDEFINITE);
        errorBar.setAction("close", new View.OnClickListener(){
            @Override
            public void onClick(View view) {
            }
        });
        errorBar.setActionTextColor(Color.WHITE);
        errorBar.show();
    }

    private void requestRefresh()
    {
        sendBroadcast(broadCastIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(changePictureReceiver);
    }
}
