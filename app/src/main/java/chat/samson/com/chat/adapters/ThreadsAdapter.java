package chat.samson.com.chat.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
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
import chat.samson.com.chat.data.MessageThread;
import de.hdodenhof.circleimageview.CircleImageView;

public class ThreadsAdapter extends BaseAdapter {
    private ArrayList<MessageThread> threads;
    private LayoutInflater inflater;

    View.OnClickListener itemClickListener;

    public ThreadsAdapter(ArrayList data, Context context, View.OnClickListener clickHandler) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        threads = data;
        this.itemClickListener = clickHandler;
    }

    @Override
    public int getCount() {
        return threads.size();
    }

    @Override
    public Object getItem(int i) {
        return threads.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        final View threadItem = view == null ? inflater.inflate(R.layout.thread_item_new, null) : view;

        int numUnreadMessages = threads.get(position).getUnreadMessages();

        TextView
                lastMessage = (TextView) threadItem.findViewById(R.id.text_last_message),
                userName = (TextView) threadItem.findViewById(R.id.text_user_name);
        final CircleImageView userProfileImage = (CircleImageView) threadItem.findViewById(R.id.image_profile_picture);

        String profileFileName = threads.get(position).getUserName();
        final String userNameStr = profileFileName;
        profileFileName = Character.toUpperCase(profileFileName.charAt(0))+profileFileName.substring(1) + ".png";


        File userProfilePic = new File(new MediaHandler(threadItem.getContext()).getProfilePicturesDir(),  profileFileName);
        final Bitmap profileBitmap = userProfilePic.exists() ? BitmapFactory.decodeFile(userProfilePic.getAbsolutePath()) :
                BitmapFactory.decodeResource(threadItem.getResources(), R.mipmap.ic__user);
        userProfileImage.setImageBitmap(profileBitmap);
        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(threadItem.getContext(), profileBitmap, userNameStr);
            }
        });


        if (numUnreadMessages > 0) {
            TextView unreadMessages = (TextView) threadItem.findViewById(R.id.text_unread_messages);
            unreadMessages.setVisibility(View.VISIBLE);
            unreadMessages.setText(Integer.toString(numUnreadMessages));
        }

        lastMessage.setText(threads.get(position).getLastMessage());
        userName.setText(threads.get(position).getUserName());

        threadItem.setTag(new Integer(position));
        threadItem.setOnClickListener(itemClickListener);

        return threadItem;
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
