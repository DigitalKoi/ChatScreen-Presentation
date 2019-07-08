package service.didi.com.offerdunkan.chat.presentation.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import service.didi.com.offerdunkan.R;
import service.didi.com.offerdunkan.base.BaseAdapter;
import service.didi.com.offerdunkan.chats.domain.model.Message;

public class MessageAdapter extends BaseAdapter<Message, RecyclerView.ViewHolder> {

    private static  final int MSG_TYPE_LEFT = 0;
    private static  final int MSG_TYPE_RIGHT = 1;

    private Context context;
    private String otherUserID;
    private int countNotSeenMessages = 100;
    private OnItemClickListener onItemClickListener;

    public MessageAdapter(
            Context context, List<Message> list, String otherUserID, OnItemClickListener onItemClickListener) {
        super(list);
        this.onItemClickListener = onItemClickListener;
        this.context = context; this.otherUserID = otherUserID;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_LEFT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_left, parent, false);
            return new MessageOtherUserViewHolder(view);
        } else if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_right, parent, false);
            return new MessageCurrentUserViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Message message = getItem(position);

        if (viewHolder instanceof MessageCurrentUserViewHolder) {
            /**
             *  Current user views
             */
            MessageCurrentUserViewHolder messageCurrentUserViewHolder = (MessageCurrentUserViewHolder) viewHolder;
            if (TextUtils.isEmpty(message.getPhotoUrl())) {
                messageCurrentUserViewHolder.photo.setVisibility(View.GONE);
                messageCurrentUserViewHolder.message.setVisibility(View.VISIBLE);
                messageCurrentUserViewHolder.message.setText(message.getText());
            } else {
                messageCurrentUserViewHolder.message.setVisibility(View.GONE);
                messageCurrentUserViewHolder.photo.setVisibility(View.VISIBLE);
                Glide.with(context).load(message.getPhotoUrl())
                        .apply(new RequestOptions()
                        .centerCrop()
//                        .override(500, 500)
                        .placeholder(R.drawable.image_load))
                        .into(messageCurrentUserViewHolder.photo);
                messageCurrentUserViewHolder.photo.setOnClickListener(v ->
                        onItemClickListener.onItemClick(v, position));
            }
            messageCurrentUserViewHolder.time.setText(message.getDateTime());
            // Logic unread message showing
            // Example: 65 count of items, count of unread messages 4,
            // then position show unread marker is 62 int notCheckPosition = getItemCount() - 1 - countNotSeenMessages;
            boolean unread = countNotSeenMessages > 0 && position > (getItemCount() - 1 - countNotSeenMessages);
            Glide.with(context).load(unread ? R.drawable.ic_unchecked : R.drawable.ic_checked)
                    .into(messageCurrentUserViewHolder.seen);

        } else if (viewHolder instanceof MessageOtherUserViewHolder) {
            /**
             *  Other user views
             */
            MessageOtherUserViewHolder messageOtherUserViewHolder = (MessageOtherUserViewHolder) viewHolder;
            if (TextUtils.isEmpty(message.getPhotoUrl())) {
                messageOtherUserViewHolder.photo.setVisibility(View.GONE);
                messageOtherUserViewHolder.message.setVisibility(View.VISIBLE);
                messageOtherUserViewHolder.message.setText(message.getText());
            } else {
                messageOtherUserViewHolder.message.setVisibility(View.GONE);
                messageOtherUserViewHolder.photo.setVisibility(View.VISIBLE);
                Glide.with(context).load(message.getPhotoUrl())
                        .apply(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.image_load))
                        .into(messageOtherUserViewHolder.photo);
                messageOtherUserViewHolder.photo.setOnClickListener(v ->
                        onItemClickListener.onItemClick(v, position));
            }
            messageOtherUserViewHolder.time.setText(message.getDateTime());
        }
    }

//    private CharSequence generateTextMessage(Message message) {
//        return EmojiCompat.get().process(message.getText());
//    }

    @Override
    public int getItemViewType(int position) {
        String senderId = getItem(position).getSenderId();
        return otherUserID.equals(senderId) ? MSG_TYPE_LEFT : MSG_TYPE_RIGHT;
    }

    public void addMessages(List<Message> messages) {
        MessageDiffCallback diffCallback = new MessageDiffCallback(getItems(), messages);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

//        getItems().clear();
        getItems().addAll(messages);
        diffResult.dispatchUpdatesTo(this);
    }

    public void setCountNotSeenMessages(int count) {
        countNotSeenMessages = count;
        notifyDataSetChanged();
    }

    class MessageCurrentUserViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message) TextView  message;
        @BindView(R.id.time)    TextView  time;
        @BindView(R.id.seen)    ImageView seen;
        @BindView(R.id.photo)   ImageView photo;

        MessageCurrentUserViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    class MessageOtherUserViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message) TextView  message;
        @BindView(R.id.time)    TextView  time;
        @BindView(R.id.photo)   ImageView photo;

        MessageOtherUserViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }
}
