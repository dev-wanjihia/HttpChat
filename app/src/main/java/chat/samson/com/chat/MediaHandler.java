package chat.samson.com.chat;

/**
 * Created by Wanjihia on 4/14/17 8:23 PM.
 */

import android.content.Context;
import android.os.Environment;
import android.os.Bundle;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaHandler extends AsyncTask<Bundle, Integer, Boolean[]>
{
    /***************************************************************************
     * Download my profile picture, user profile pictures and media such as    *
     * documents e.g pdf                                                       *
     * My contacts' and my profile picture -> save it in the internal storage  *
     * media -> download and write to external files media if there is enough  *
     * space and the media is writable if the external storage is unavailable  *
     * use the internal storage if there is enough space.                      *
     * thumbnails will be stored in the internal storage and updated later.
     *
     *
     *
     ***************************************************************************/

    public static final String WHICH_KEY = "type_of_download";

     static final String PROFILE_PICTURE_FILE_NAME = "profile_picture.png";

    public static final int MY_PROFILE_PIC = 1;
    public static final int USER_PROFILE_PIC = 2;
    public static final int MEDIA = 3;

    public static final int MEDIA_IMAGE = 4;
    public static final int MEDIA_VIDEO = 5;
    public static final int MEDIA_AUDIO = 6;
    public static final int MEDIA_DOCUMENT = 7;

    public static final String DOWNLOAD_BASE_URL_KEY = "base_url_for_downloads";
    public static final String FILE_LINK_URL = "file_url_for_download";
    public static final String FILES_LINK_URL = "files_urls_for_download";
    public static final String DOWNLOAD_THUMB_ONLY = "download_thumb";
    public static final String FILE_PATHS_ARRAY = "file_paths";

    private Context context;

    public MediaHandler(Context downloadContext)
    {
        context = downloadContext;
    }

    @Override
    protected Boolean[] doInBackground(Bundle ... bundles)
    {
        /**
         * @bundles[0] provides the urls, token, username and location to save the media to.
         */

        Bundle downloadData = bundles[0];
        int which = downloadData.getInt(WHICH_KEY);
        Boolean[] results = null;
        String baseURL = downloadData.getString(DOWNLOAD_BASE_URL_KEY);

        switch(which)
        {
            case MY_PROFILE_PIC:
                results = new Boolean[1];

                String fileURL = downloadData.getString(FILE_LINK_URL);

                if(downloadData.containsKey(DOWNLOAD_THUMB_ONLY) && downloadData.getBoolean(DOWNLOAD_THUMB_ONLY))
                    fileURL = "t_" + fileURL;

                File profilePicture = new File(getPersonalDataPath(), PROFILE_PICTURE_FILE_NAME);

                if(!profilePicture.exists())
                {
                    try
                    {
                        profilePicture.createNewFile();
                    }
                    catch(IOException ioException)
                    {
                        ioException.printStackTrace();
                    }
                }

                results[0] = startDownload(baseURL + "/" +fileURL, profilePicture);
                break;
            case USER_PROFILE_PIC:
                String[] filesURL = downloadData.getStringArray(FILES_LINK_URL);
                String[] filePaths = downloadData.getStringArray(FILE_PATHS_ARRAY);

                results = new Boolean[filesURL.length];

                for(int i = 0; i < filesURL.length; i++)
                {
                    if(downloadData.containsKey(DOWNLOAD_THUMB_ONLY) && downloadData.getBoolean(DOWNLOAD_THUMB_ONLY))
                        filesURL[i] = "t_" +  filesURL[i];

                    File saveToFile = new File(getProfilePicturesDir(), filePaths[i]);
                    results[i] = startDownload(baseURL + "/" + filesURL[i], saveToFile);
                }
                break;
        }
        return results;
    }

    @Override
    protected void onProgressUpdate(Integer ... values)
    {
        super.onProgressUpdate(values);
    }

    private boolean startDownload(String url, File destination) {
        try {
            URL downloadURL = new URL(url);
            HttpURLConnection downloadConnection = (HttpURLConnection) downloadURL.openConnection();

            downloadConnection.setDoInput(true);

            InputStream downloadStream = downloadConnection.getInputStream();
            FileOutputStream saveToStream = new FileOutputStream(destination);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = downloadStream.read(buffer)) != -1) {
                saveToStream.write(buffer, 0, bytesRead);
            }

            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public String getInternalStoragePath()
    {
        File  internalDir =new File(context.getFilesDir().getPath(), "HttpChat");

        if(!internalDir.exists())
            internalDir.mkdirs();

        return internalDir.getAbsolutePath();
    }

    public String getPersonalDataPath()
    {
        File personalDir = new File(getInternalStoragePath(), "personal_data");

        if(!personalDir.exists())
            personalDir.mkdirs();

        return personalDir.getAbsolutePath();
    }

    public String getProfilePicturesDir()
    {
        File profilePicDir = new File(getInternalStoragePath(), "profile pictures");

        if(!profilePicDir.exists())
            profilePicDir.mkdirs();

        return profilePicDir.getAbsolutePath();
    }
}
