package service.didi.com.offerdunkan.chat.presentation;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.fxn.pix.Pix;
import com.fxn.utility.PermUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import service.didi.com.offerdunkan.R;
import service.didi.com.offerdunkan.base.BaseActivityMenu;
import service.didi.com.offerdunkan.chat.presentation.adapter.AnswerAdapter;
import service.didi.com.offerdunkan.chat.presentation.adapter.MessageAdapter;
import service.didi.com.offerdunkan.chat.presentation.decoration.HorizontalMarginItemDecoration;
import service.didi.com.offerdunkan.chat.presentation.decoration.VerticalMarginItemDecoration;
import service.didi.com.offerdunkan.chats.domain.model.Message;
import service.didi.com.offerdunkan.chats.domain.model.UserEntity;
import service.didi.com.offerdunkan.chats.presentation.ChatsActivity;
import service.didi.com.offerdunkan.db.city.CityEntity;
import service.didi.com.offerdunkan.extensions.ContextExtensionKt;
import service.didi.com.offerdunkan.saletype.domain.model.ProfileAllowed;
import service.didi.com.offerdunkan.utils.KeyboardUtil;

import static service.didi.com.offerdunkan.chat.presentation.FullScreenPhotoActivity.KEY_PHOTO_URL;

public class ChatActivity extends BaseActivityMenu implements ChatView {

    public static final String KEY_USER_ID = "key.user.id";
    public static final String KEY_ROOM_ID = "key.room.id";


    @InjectPresenter ChatPresenter chatPresenter;

    @BindView(R.id.toolbar)         Toolbar         toolbar;
    @BindView(R.id.progress)        CardView        progress;
    @BindView(R.id.answer_recycler) RecyclerView    answerRecyclerView;
    @BindView(R.id.chat_recycler)   RecyclerView    chatRecyclerView;
    @BindView(R.id.profile_image)   ImageView       otherUserProfilePhoto;
    @BindView(R.id.online)          CircleImageView online;
    @BindView(R.id.username)        TextView        otherUserName;
    @BindView(R.id.message_edit)    EditText        textField;
    @BindView(R.id.send)            ImageView       send;

    private View.OnLayoutChangeListener onLayoutChangeListener;
    private RecyclerView.OnScrollListener onScrollListener;

    private AtomicInteger verticalScrollOffset = new AtomicInteger(0);

    private MessageAdapter  messageAdapter;
    private AnswerAdapter   answerAdapter;
    private TextWatcher     watcher;
    private Menu            menu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);
        setupToolbar();

        setupRecyclerAnswer();
        setListenerEditText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String otherUserId = bindPresenter();
        setupRecyclerMessage(otherUserId);
    }

    @Override
    protected void onPause() {
        chatPresenter.unsubscribeListeners();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        textField.removeTextChangedListener(watcher);
        chatRecyclerView.removeOnLayoutChangeListener(onLayoutChangeListener);
        chatRecyclerView.removeOnScrollListener(onScrollListener);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.menu = menu;
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.block_user:
                List<Message> items = messageAdapter.getItems();
                String lastMessage = items.size() > 0 ? items.get(items.size()-1).getText() : "";
                long timestamp = items.size() > 0 ? items.get(items.size()-1).getTimestamp() : System.currentTimeMillis();
                chatPresenter.callBlockUser(
                        TextUtils.equals(item.getTitle(), getResources().getString(R.string.txt_chat_menu_block)),
                        lastMessage,
                        timestamp
                        );
                return true;
//            case R.id.delete_chat:
//                chatPresenter.deleteChat();
//                return true;
        }
        return true;
    }

    @Override
    public void showSellAllowed(ProfileAllowed profileAllowed) {

    }

    @Override
    public void searchCityResult(List<CityEntity> cityEntityList) {

    }

    @Override
    public void showProgress() {
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        progress.setVisibility(View.GONE);
    }

    @Override
    public void showError(Throwable e) {
        ContextExtensionKt.showErrorDialog(this, e);
    }

    @Override
    public void addMessages(List<Message> messages) {
        messageAdapter.addMessages(messages);
        if (messageAdapter.getItems().size() > 0)
            chatRecyclerView.scrollToPosition(messageAdapter.getItems().size()-1);
        if (!messages.isEmpty()) {
            Message message = messages.get(messages.size() - 1);
            chatPresenter.updateCurrentUserData(message.getText(), message.getTimestamp());
        }
    }

    @Override
    public void setUnreadCount(Integer unreadMessageCount) {
        messageAdapter.setCountNotSeenMessages(unreadMessageCount);
    }

    @Override
    public void showAnswer(List<String> answers) {
        answerAdapter.add(answers);
    }

    @Override
    public void showOtherUserData(UserEntity user) {
        Glide.with(this)
                .load(user.getPhoto())
                .apply(new RequestOptions().override(36).placeholder(R.drawable.ic_account_empty))
                .into(otherUserProfilePhoto);
        otherUserName.setText(user.getName());
    }

    @Override
    public void changeBlockStatus(boolean blocked) {
        MenuItem menuItem = menu.findItem(R.id.block_user);
        menuItem.setTitle(getResources().getString(
                blocked ? R.string.txt_chat_menu_unblock : R.string.txt_chat_menu_block
        ));
    }

    @Override
    public void showOtherUserOnline(boolean isOnline) {
        online.setImageResource(isOnline ? R.color.green : R.color.gray_77);
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.send)
    void sendMessage() {
        String message = textField.getText().toString();
        chatPresenter.sendMessage(message);
        textField.setText(null);
        KeyboardUtil.hideKeyboard(textField);
    }

    @OnClick(R.id.addphoto)
    void addPhoto() {
        showImagePicker();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ChatsActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == 1005) {
            ArrayList<String> photos = data.getStringArrayListExtra(Pix.IMAGE_RESULTS);
            chatPresenter.sendPhoto(photos);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == PermUtil.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImagePicker();
            } else {
                Toast.makeText(ChatActivity.this, "Approve permissions to open Pix ImagePicker", Toast.LENGTH_LONG).show();
            }
        }
    }


    private String bindPresenter() {
        String otherUserId = getIntent().getStringExtra(KEY_USER_ID);
        String roomId = getIntent().getStringExtra(KEY_ROOM_ID);

        if (!TextUtils.isEmpty(otherUserId) && !TextUtils.isEmpty(roomId))
            chatPresenter.getOtherUserInfo(otherUserId, roomId);
        else finish();

        return otherUserId;
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener((view) -> onBackPressed());
    }

    private void setupRecyclerMessage(String otherUserId) {
        if (messageAdapter != null) return;
        messageAdapter = new MessageAdapter(this, new ArrayList<>(), otherUserId, (view, position) -> {
            showFullScreenActivity(messageAdapter.getItems().get(position).getPhotoUrl());
        });
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        chatRecyclerView.addItemDecoration(new VerticalMarginItemDecoration((int) getResources().getDimension(R.dimen.dimen_4dp)));
        chatRecyclerView.setAdapter(messageAdapter);

        // Logic for save position after show/hide keyboard
        if (onLayoutChangeListener == null) {
            onLayoutChangeListener = (view, i, i1, i2, bottom, i4, i5, i6, oldBottom) -> {
                int y = oldBottom - bottom;
                if (Math.abs(y) > 0) {
                    chatRecyclerView.post(() -> {
                        if (y > 0 || Math.abs(verticalScrollOffset.get()) >= Math.abs(y)) {
                            chatRecyclerView.scrollBy(0, y);
                        } else {
                            chatRecyclerView.scrollBy(0, verticalScrollOffset.get());
                        }
                    });
                }
            };
        }

        if (onScrollListener == null) {
            onScrollListener = new RecyclerView.OnScrollListener() {
                private AtomicInteger state = new AtomicInteger(RecyclerView.SCROLL_STATE_IDLE);

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    switch (newState) {
                        case RecyclerView.SCROLL_STATE_IDLE:
                            if (!state.compareAndSet(RecyclerView.SCROLL_STATE_SETTLING, newState)) {
                                state.compareAndSet(RecyclerView.SCROLL_STATE_DRAGGING, newState);
                            }
                            break;
                        case RecyclerView.SCROLL_STATE_DRAGGING:
                            state.compareAndSet(RecyclerView.SCROLL_STATE_IDLE, newState);
                            break;
                        case RecyclerView.SCROLL_STATE_SETTLING:
                            state.compareAndSet(RecyclerView.SCROLL_STATE_DRAGGING, newState);
                            break;
                    }
                }

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (state.get() != RecyclerView.SCROLL_STATE_IDLE) {
                        verticalScrollOffset.getAndAdd(dy);
                    }
                }
            };
        }

        chatRecyclerView.addOnLayoutChangeListener(onLayoutChangeListener);
        chatRecyclerView.addOnScrollListener(onScrollListener);
    }

    private void setupRecyclerAnswer() {
        answerAdapter = new AnswerAdapter(
                new ArrayList<>(),
                (view, position) -> textField.setText(answerAdapter.getItem(position)));

        answerRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        answerRecyclerView.addItemDecoration(new HorizontalMarginItemDecoration((int) getResources().getDimension(R.dimen.dimen_4dp)));
        answerRecyclerView.setAdapter(answerAdapter);
    }

    private void setListenerEditText() {
        watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) send.setVisibility(View.VISIBLE);
                else send.setVisibility(View.GONE);
            }
        };

        textField.addTextChangedListener(watcher);
    }

    private void showImagePicker() {
        Pix.start(this, 1005, 1);
    }

    private void showFullScreenActivity(String path) {
        Intent intent = new Intent(this, FullScreenPhotoActivity.class);
        intent.putExtra(KEY_PHOTO_URL, path);
        startActivity(intent);
    }
}
