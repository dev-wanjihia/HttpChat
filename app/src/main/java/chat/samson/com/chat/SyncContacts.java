package chat.samson.com.chat;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.*;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import chat.samson.com.chat.data.DatabaseAdapter;
import chat.samson.com.chat.data.User;
import chat.samson.com.chat.data.UserProfile;

public abstract class SyncContacts
{
    ContentResolver resolver;

    DatabaseAdapter dbAdapter;

    NetworkThread networkThread;

    Bundle data;

    UserProfile userProfile;

    Context context;

    private MediaHandler mediaHandler;

    //These are the profile pictures that we are going to download and the base url.
    private ArrayList<String> profilePictures, profilePictureLinks;
    private String baseURL;

    public abstract void onComplete(int status);
    public abstract void onStart();

    public void start(Context context)
    {
        this.context = context;
        profilePictureLinks = new ArrayList<>();
        profilePictures = new ArrayList<>();
        init();
    }

    private void init()
    {
        data = new Bundle();

        networkThread = new NetworkThread();

        dbAdapter = new DatabaseAdapter(context);

        userProfile = dbAdapter.getUserProfile();

        resolver = context.getContentResolver();

        userProfile = dbAdapter.getUserProfile();

        insertUserNames();
    }

    private void insertUserNames()
    {
        onStart();

        Cursor emailContacts = context.getContentResolver().query(Data.CONTENT_URI, new String[]{Data.DATA1},
                              Data.MIMETYPE + " = '" + CommonDataKinds.Email.CONTENT_ITEM_TYPE
                                      + "' and not " + Data.DATA1 + " = '" + dbAdapter.getUserProfile().getEmail() +"'",
                              null, null);

        String[] emails = new String[emailContacts.getCount()];

        if(emailContacts.moveToFirst())
        {
            do
            {
                emails[emailContacts.getPosition()] = emailContacts.getString(emailContacts.getColumnIndex(Data.DATA1)).toLowerCase();
            }
            while(emailContacts.moveToNext());
        }

        emailContacts.close();

        data.putInt(NetworkThread.ACTION_KEY, NetworkThread.ACTION_SYNC_CONTACTS);
        data.putString(NetworkThread.USERNAME_KEY, userProfile.getUserName());
        data.putString(NetworkThread.TOKEN_KEY, userProfile.getToken());
        data.putStringArray(NetworkThread.EMAIL_ADDRESSES_KEY, emails);

        networkThread.execute(data);

        try
        {
            JSONObject results = networkThread.get();

            Log.w(getClass().getSimpleName(), results.toString());

            if(results == null)
            {
                onComplete(0);
                return;
            }

            for(String email : emails)
            {
                User temp = new User();
                JSONObject userDetails = results.getJSONObject(email);

                String profilePicture = userDetails.getString("profile_picture");

                temp.setUsername(userDetails.getString("user_name"));
                temp.setEmail(email);
                temp.setProfilePicture(profilePicture.matches("null") ? "" : profilePicture);
                temp.setInContacts(true);

                //Check if the user is signed up then insert them into the database.
                if(temp.isRegistered())
                {
                    //Check if the current profile picture is the same as that in the server
                    User localUser = dbAdapter.getUser(temp.getUsername());

                    //If the local profile picture is not the same as the one in the server then download
                    if(!(localUser.getProfilePicture()+"t").matches((temp.getProfilePicture() + "t")))
                    {
                        //The files will be saved in the username.png file.
                        profilePictures.add(temp.getUsername() +".png");
                        profilePictureLinks.add(temp.getProfilePicture());
                    }

                    dbAdapter.insertUser(temp);
                }
            }

            baseURL = results.getString("base_url");

            downloadPPics();

            onComplete(1);
        }
        catch(Exception ex)
        {
            onComplete(0);
            ex.printStackTrace();
        }
    }

    private void downloadPPics()
    {
        Bundle ppicsBundle = new Bundle();

        String[] profileLinks, profileFiles;

        profileFiles = new String[profilePictures.size()];
        profileLinks = new String[profilePictureLinks.size()];

        for(int i = 0; i < profilePictures.size(); i++)
        {
            profileFiles[i] = profilePictures.get(i);
            profileLinks[i] = profilePictureLinks.get(i);
        }

        ppicsBundle.putInt(MediaHandler.WHICH_KEY, MediaHandler.USER_PROFILE_PIC);
        ppicsBundle.putStringArray(MediaHandler.FILE_PATHS_ARRAY, profileFiles);
        ppicsBundle.putStringArray(MediaHandler.FILES_LINK_URL, profileLinks);
        ppicsBundle.putBoolean(MediaHandler.DOWNLOAD_THUMB_ONLY, true);
        ppicsBundle.putString(MediaHandler.DOWNLOAD_BASE_URL_KEY, baseURL);

        mediaHandler = new MediaHandler(context);

        try
        {
            mediaHandler.execute(ppicsBundle);

            Boolean[] results =  mediaHandler.get();

            for(boolean res : results)
            {
                Log.w("Status", res + "");
            }
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }


    }
}
