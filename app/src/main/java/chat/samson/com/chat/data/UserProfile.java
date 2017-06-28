package chat.samson.com.chat.data;

import android.content.ContentValues;

/**
 * Created by root on 3/15/17.
 */

public class UserProfile
{
    private String userName, email, token, profilePicture;

    public String getProfilePicture()
    {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ContentValues getValues()  {
        ContentValues result = new ContentValues();

        result.put("user_name", userName.toLowerCase());
        result.put("email", email);
        result.put("auth_token", token);
        result.put("profile_picture", profilePicture);

        return result;
    }
}
