package chat.samson.com.chat.adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import chat.samson.com.chat.MediaHandler;
import chat.samson.com.chat.R;
import chat.samson.com.chat.data.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends BaseAdapter
{
    ArrayList<User> users;
    LayoutInflater inflater;
    View.OnClickListener itemClickListener;

    public UserAdapter(ArrayList users, Context context, View.OnClickListener itemClickListener)
    {
        this.users = users;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemClickListener = itemClickListener;
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View recycleView, ViewGroup container)
    {
        final View userView = recycleView == null ? inflater.inflate(R.layout.user_item, null) : recycleView;
        final String userName = users.get(position).getUsername();

        ((TextView)userView.findViewById(R.id.text_user_name))
                         .setText(userName);
        final CircleImageView userProfile = (CircleImageView)userView.findViewById(R.id.image_profile_picture);

        File profilePicture = new File(new MediaHandler(userView.getContext()).getProfilePicturesDir(), users.get(position).getUsername() + ".png");

        final Bitmap profileBitmap = profilePicture.exists() ? BitmapFactory.decodeFile(profilePicture.getAbsolutePath()) :
                BitmapFactory.decodeResource(userView.getResources(), R.mipmap.ic__user);


        userProfile.setImageBitmap(profileBitmap);
        userProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(userView.getContext(), profileBitmap, userName);
            }
        });

        userView.setTag(new Integer(position));
        userView.setOnClickListener(itemClickListener);


        return userView;
    }

    private void showDialog(Context context, Bitmap bmp, String title)
    {
        Dialog imageDialog = new Dialog(context, android.support.design.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert);

        imageDialog.setContentView(R.layout.image_dialog);

        TextView titleView = (TextView)imageDialog.findViewById(R.id.text_title);
        ImageView imageView = (ImageView)imageDialog.findViewById(R.id.viewing_image);

        imageView.setImageBitmap(bmp);
        titleView.setText(title);

        imageDialog.setCancelable(true);
        imageDialog.setCanceledOnTouchOutside(true);

        imageDialog.show();
    }
}
