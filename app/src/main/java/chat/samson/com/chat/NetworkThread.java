package chat.samson.com.chat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.File;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;


public class NetworkThread extends AsyncTask<Bundle, Integer, JSONObject> {

    /***********************************************************
     * Handle all the network operations.                      *
     * 1. signing up. -> email, username.                      *
     * 2. verification. -> verification code.                  *
     * 3. sync-contacts -> email addresses.                    *
     * 4. send-messages -> username, message, message time.    *
     * 5. check for messages.                                  *
     * 6. receive-messages -> username, message, message time. *
     * 7. upload profile picture -> file profile picture       *
     ***********************************************************/


    public static final String BASE_URL = "http://192.168.43.139/chat/";

    public static final String ACTION_KEY = "ACTION";


    //Sign up keys
    public static final String EMAIL_KEY = "email";
    public static final String USERNAME_KEY = "username";
    public static final String PROFILE_PIC_KEY = "profile_picture";
    public static final String PROFILE_PICTURE_THUMB_KEY = "profile_picture_thumb";

    //verification keys
    public static final String VERIFICATION_KEY = "verification_code";

    //check message keys
    public static final String TOKEN_KEY = "token";

    //send message keys
    public static final String USERNAME_TO_KEY = "username_to";
    public static final String MESSAGE_KEY = "message";
    public static final String TIME_KEY = "msg_time";

    //sync_contact keys
    public static final String EMAIL_ADDRESSES_KEY = "emails";

    //Upload profile picture keys
    public static final String PROFILE_PIC_PATH_KEY = "profile_picture_path";

    public static final String REMOVE_PROFILE_PIC_KEY = "remove_profile_picture";

    public static final int ACTION_SIGN_UP = 0;
    public static final int ACTION_VERIFY = 1;
    public static final int ACTION_CHECK_MESSAGE = 2;
    public static final int ACTION_GET_MESSAGES = 3;
    public static final int ACTION_SEND_MESSAGE = 4;
    public static final int ACTION_SYNC_CONTACTS = 5;
    public static final int ACTION_RESEND_KEY = 6;
    public static final int ACTION_REQUEST_EMAIL = 7;
    public static final int ACTION_UPLOAD_PROFILE = 8;
    public static final int ACTION_GET_PROFILE_PIC = 9;
    public static final int ACTION_DELETE_ACCOUNT = 10;

    public OnFinishListener onFinishListener;


    private final String[] urls = new String[]{
            "sign_up.php", "verify.php",
            "check_messages.php", "get_messages.php",
            "send_message.php", "sync_contacts.php",
            "resend_key.php", "request_email.php",
            "update_profile_picture.php", "get_profile_pictures.php",
            "delete_account.php"};

    private int progress;
    private OutputStream outputStream;
    private String lineFeed = "\r\n", boundary = "===" + System.currentTimeMillis() + "===";
    private PrintWriter writer;
    private HttpURLConnection connection;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progress = 0;
    }

    @Override
    protected JSONObject doInBackground(Bundle ... params) {
        Bundle data = params[0];

        int action = data.getInt(ACTION_KEY);

        return getResult(urls[action], data);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }


    private JSONObject getResult(String url, Bundle data) {
        try
        {
            connection = (HttpURLConnection) (new URL(BASE_URL + url).openConnection());

            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(3000);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            outputStream = connection.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

            addFormField(USERNAME_KEY, data.getString(USERNAME_KEY).toLowerCase());

            switch (data.getInt(ACTION_KEY)) {
                case ACTION_SIGN_UP:
                    addFormField(EMAIL_KEY, data.getString(EMAIL_KEY).toLowerCase());
                    addFormField(PROFILE_PIC_KEY, data.getString(PROFILE_PIC_KEY));
                    break;

                case ACTION_VERIFY:
                    addFormField(VERIFICATION_KEY, data.getString(VERIFICATION_KEY));
                    break;

                case ACTION_CHECK_MESSAGE:
                case ACTION_GET_MESSAGES:
                    addFormField(TOKEN_KEY, data.getString(TOKEN_KEY));
                    break;

                case ACTION_SEND_MESSAGE:
                    addFormField(TOKEN_KEY, data.getString(TOKEN_KEY));
                    addFormField(USERNAME_TO_KEY, data.getString(USERNAME_TO_KEY).toLowerCase());
                    addFormField(MESSAGE_KEY, data.getString(MESSAGE_KEY));
                    break;

                case ACTION_SYNC_CONTACTS:
                    addFormField(TOKEN_KEY, data.getString(TOKEN_KEY));
                    String[] contactEmails = data.getStringArray(EMAIL_ADDRESSES_KEY);

                    for (int i = 0; i < contactEmails.length; i++)
                        addFormField(EMAIL_ADDRESSES_KEY + "[" + i + "]", contactEmails[i].toLowerCase());

                    break;

                case ACTION_RESEND_KEY:
                    addFormField(EMAIL_KEY, data.getString(EMAIL_KEY));
                    break;

                case ACTION_REQUEST_EMAIL:
                    addFormField(TOKEN_KEY, data.getString(TOKEN_KEY));
                    addFormField(USERNAME_TO_KEY, data.getString(USERNAME_TO_KEY).toLowerCase());
                    break;

                case ACTION_UPLOAD_PROFILE:
                    addFormField(TOKEN_KEY, data.getString(TOKEN_KEY));
                    addFormField(EMAIL_KEY, data.getString(EMAIL_KEY));

                    if (data.containsKey(REMOVE_PROFILE_PIC_KEY) && data.getBoolean(REMOVE_PROFILE_PIC_KEY)) {
                        if(onFinishListener != null)
                            onFinishListener.onFinish();
                        break;
                    }

                    File profilePicture = new File(data.getString(PROFILE_PIC_PATH_KEY));
                    File profileThumb = new File(profilePicture.getParentFile().getPath(), "thumb_" + profilePicture.getName());

                    if(!profileThumb.exists())
                        profileThumb.createNewFile();

                    Bitmap profileThumbBitmap = BitmapFactory.decodeFile(profilePicture.getAbsolutePath());
                    profileThumbBitmap.compress(Bitmap.CompressFormat.PNG, 1, new FileOutputStream(profileThumb));

                    addFileField(PROFILE_PIC_KEY, profilePicture);
                    addFileField(PROFILE_PICTURE_THUMB_KEY, profileThumb);

                    break;

                case ACTION_GET_PROFILE_PIC:
                    addFormField(TOKEN_KEY, data.getString(TOKEN_KEY));

                    if (data.containsKey(NetworkThread.EMAIL_ADDRESSES_KEY)) {
                        String[] emails = data.getStringArray(NetworkThread.EMAIL_ADDRESSES_KEY);

                        int i = 0;

                        if (emails != null)
                            while (i < emails.length) {
                                addFormField(EMAIL_ADDRESSES_KEY + "[" + i + "]", emails[i].toLowerCase());
                                i++;
                            }
                    }
                    break;

                case ACTION_DELETE_ACCOUNT:
                    addFormField(NetworkThread.TOKEN_KEY, data.getString(TOKEN_KEY));
                    addFormField(NetworkThread.VERIFICATION_KEY, data.getString(VERIFICATION_KEY));
                    break;
            }


            writer.append("--" + boundary + "--")
                    .append(lineFeed).flush();

            outputStream.flush();
            writer.close();
            outputStream.close();

            Log.w("ResponseMessage" +
                    "", connection.getResponseMessage());

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line, results = "";

            while ((line = reader.readLine()) != null) {
                results += line;
            }

            reader.close();

            Log.w("NetworkResults", results);

            if(onFinishListener != null)
                onFinishListener.onFinish();

            return connection.getResponseCode() == HttpURLConnection.HTTP_OK ? new JSONObject(results) : null;
        } catch (java.net.ConnectException exception) {
            exception.printStackTrace();
            return null;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

            if (writer != null) {
                writer.close();
            }
        }
    }

    private void addFormField(String name, String value) {
        writer.append("--" + boundary).append(lineFeed)
                .append("Content-Disposition: form-data;")
                .append(" name=\"" + name + "\"" + lineFeed)
                .append("Content-Type: text/plain; charset=UTF-8")
                .append(lineFeed + lineFeed).append(value).append(lineFeed)
                .flush();
    }

    private void addFileField(String fieldName, File filePart) throws IOException
    {
        String fileName = filePart.getName();

        writer.append("--").append(boundary).append(lineFeed)
                .append("Content-Disposition: form-data; name=\"" + fieldName + "\"; ")
                .append("filename=\"").append(fileName).append("\"")
                .append(lineFeed);

        writer.append("Content-Type: ").append(HttpURLConnection.guessContentTypeFromName(fileName))
                .append(lineFeed)
                .append("Content-Transfer-Encoding: binary")
                .append(lineFeed)
                .append(lineFeed)
                .flush();

        FileInputStream fileInputStream = new FileInputStream(filePart);

        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        fileInputStream.close();
        outputStream.flush();
        writer.append(lineFeed);
        writer.flush();
    }

    public void setOnFinishListener(OnFinishListener onFinishListener)
    {
        this.onFinishListener = onFinishListener;
    }


    //These two interfaces will be used to monitor the progress and the completion of the network task
    public interface OnFinishListener
    {
        void onFinish();
    }

    public interface OnProgressUpdatedListener
    {
        void onProgress(int progress);
    }
}
