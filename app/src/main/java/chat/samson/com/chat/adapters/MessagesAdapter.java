package chat.samson.com.chat.adapters;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import chat.samson.com.chat.R;
import chat.samson.com.chat.UserChat;
import chat.samson.com.chat.data.Message;


public class MessagesAdapter extends BaseAdapter
{
    private ArrayList<Message> messages;
    private LayoutInflater inflater;
    private View.OnLongClickListener longClickListener;
    private boolean enableAnimation = true;

    public MessagesAdapter (ArrayList messages)
    {
        this.messages = messages;
        inflater = (LayoutInflater)(UserChat.USER_CHAT_CONTEXT).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return messages.indexOf(messages.get(i));
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        LinearLayout chatView = view == null ? (LinearLayout) inflater.inflate(R.layout.message_item_new, null) : (LinearLayout) view;

        Message current = messages.get(i);

        TextView msgTime = (TextView)chatView.findViewById(R.id.text_message_time),
                msgText = (TextView)chatView.findViewById(R.id.text_message_container);

        msgText.setText(current.getMessage());
        msgTime.setText(current.getTime().length() == 14 ? current.getTime().substring(8, 10) + ":" + current.getTime().substring(10, 12)  : "");

        chatView.setTag(new String[]{"messages", i + ""});

        if(current.getType() == Message.TYPE_RECEIVED)
        {
            chatView.setGravity(Gravity.LEFT);
            chatView.getChildAt(0).setBackgroundResource(R.drawable.message_sent_bg);
            if(enableAnimation) chatView.startAnimation(AnimationUtils.loadAnimation(chatView.getContext(), R.anim.inflate_animation_from_left));
        }
        else if(current.getType() == Message.TYPE_SENT)
        {
            chatView.setGravity(Gravity.RIGHT);
            chatView.getChildAt(0).setBackgroundResource(R.drawable.message_received_bg);
            if(enableAnimation) chatView.startAnimation(AnimationUtils.loadAnimation(chatView.getContext(), R.anim.inflate_animation_from_right));
        }

        chatView.setOnLongClickListener(longClickListener);

        return chatView;
    }

    public void setAnimationOnAdd(boolean animate)
    {
        enableAnimation = animate;
    }

    public void setLongClickListener(View.OnLongClickListener onLongClickListener)
    {
        this.longClickListener = onLongClickListener;
    }


}
