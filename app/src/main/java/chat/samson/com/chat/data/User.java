package chat.samson.com.chat.data;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Log;

import org.w3c.dom.Text;

/**
 * Created by root on 3/17/17.
 */

public class User
{
    String email;

    String username;
    String profilePicture;
    boolean inContacts;

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getUsername()
    {
        return TextUtils.isEmpty(username) ? "" : Character.toUpperCase(username.charAt(0)) + username.substring(1);
    }

    public void setUsername(String username)
    {
        
        this.username = username;
    }

    public String getEmail()
    {
        
        return email;
    }

    public void setEmail(String email)
    {
        
        this.email = email;
    }

    public ContentValues genValues()
    {
        ContentValues result = new ContentValues();

        result.put("user_name", username.toLowerCase());
        result.put("email", email);
        result.put("in_contacts", inContacts ? 1 : 0);
        result.put("profile_picture", profilePicture);

        return result;
    }

    public boolean isRegistered()
    {
        return !username.matches("999");
    }

    public void setInContacts(boolean inContacts)
    {
        this.inContacts = inContacts;
    }

    public boolean inContacts()
    {
        return inContacts;
    }

}
