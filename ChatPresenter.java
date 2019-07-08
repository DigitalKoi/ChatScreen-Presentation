package service.didi.com.offerdunkan.chat.presentation;

import android.text.TextUtils;

import com.arellomobile.mvp.InjectViewState;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import service.didi.com.offerdunkan.base.App;
import service.didi.com.offerdunkan.base.BasePresenterMenu;
import service.didi.com.offerdunkan.chat.di.ChatModule;
import service.didi.com.offerdunkan.chat.domain.interactor.ChatInteractor;
import service.didi.com.offerdunkan.chats.domain.model.Message;
import service.didi.com.offerdunkan.dagger.scopes.ChatScope;
import service.didi.com.offerdunkan.chats.domain.model.UserEntity;
import service.didi.com.offerdunkan.db.city.repository.CityRepository;
import service.didi.com.offerdunkan.utils.ProfileAllowedUtil;
import service.didi.com.offerdunkan.utils.SearchCityUtil;

/*
    Logic for listening and sending messages
    ________________________________________________________________________________________________________
   |    1. Get users data, save to local db and return other user data for showing in UI                    |
   |    2. Get once all messages                                                                            |
   |    3. Subscribe to Firebase Users current room data (for current and other users changes)              |
   |    4. Receive changes from current and show new messages                                               |
   |    5. Send unread message count 0
   |    6. Receive changes from other user and change seen messages indicator                               |
   |    7. Send new Message to Firebase db and update two users data in Firebase database Users model room  |
    --------------------------------------------------------------------------------------------------------
 */

@ChatScope
@InjectViewState
public class ChatPresenter extends BasePresenterMenu<ChatView> {

    @Inject ChatInteractor chatInteractor;
    @Inject CityRepository cityRepository;

    private UserEntity otherUser;

    ChatPresenter() {
        App.getApp().plusChatModule(new ChatModule()).inject(this);
    }

    @Override
    protected void profileAllowed(FirebaseUser user) {
        ProfileAllowedUtil.profileAllowed(user, this, chatInteractor);
    }

    @Override
    protected void searchCity(String searchText) {
        SearchCityUtil.searchCity(searchText, this, cityRepository);
    }

    void getOtherUserInfo(String otherUserId, String roomId) {
        if (otherUser != null) {
            subscribeToChatData();
        } else {
            getViewState().showProgress();
            chatInteractor.getUsersInfo(otherUserId, roomId)
                    .subscribe(new DisposableSingleObserver<UserEntity>() {
                        @Override
                        public void onSuccess(UserEntity userEntity) {
                            otherUser = userEntity;
                            getViewState().showOtherUserData(otherUser);
                            getAnswers();
                            prepareUsersDataAndRoom();
                        }

                        @Override
                        public void onError(Throwable e) {
                            getViewState().showError(e);
//                            getOtherUserInfo(otherUserId, roomId);
                            getViewState().hideProgress();
                        }
                    });
        }
    }

    private void subscribeToChatData() {
        subscribeCurrentUserData();
        subscribeOtherUserData();
        subscribeOtherUserOnline();
    }

    private void prepareUsersDataAndRoom() {
        getViewState().showProgress();
        chatInteractor.prepareUsersDataAndRoom()
                .subscribe(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        checkOtherUserBlockedFirebase();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
    }

    private void checkOtherUserBlockedFirebase() {
        getViewState().showProgress();
        chatInteractor.checkBlockedStatusFirebase()
            .subscribe(new DisposableSingleObserver<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    getViewState().hideProgress();
                    getMessages();
                }

                @Override
                public void onError(Throwable e) {
                    getViewState().hideProgress();
                }
            });
    }

    void unsubscribeListeners() {
        chatInteractor.unsubscribeListeners();
    }

    void sendMessage(String message) {
        if (chatInteractor.isBlockedCurrentUser()) {
            getViewState().showMessage("You are blocked by other user");
        } else if (!TextUtils.isEmpty(message)) {
            getViewState().showProgress();
            chatInteractor.sendMessage(message)
                    .subscribe(new DisposableSingleObserver<List<Message>>() {
                        @Override
                        public void onSuccess(List<Message> messages) {
                            getViewState().addMessages(messages);
                            getViewState().hideProgress();
                        }

                        @Override
                        public void onError(Throwable e) {
                            getViewState().showError(e);
                            getViewState().hideProgress();
                        }
                    });
        }
    }

    void sendPhoto(ArrayList<String> photos) {
        if (chatInteractor.isBlockedCurrentUser()) {
            getViewState().showMessage("You are blocked by other user");
        } else {
            getViewState().showProgress();

            chatInteractor.sendPhoto(photos)
                    .subscribe(new DisposableSingleObserver<List<Message>>() {
                        @Override
                        public void onSuccess(List<Message> messages) {
                            getViewState().addMessages(messages);
                            getViewState().hideProgress();
                        }

                        @Override
                        public void onError(Throwable e) {
                            getViewState().showError(e);
                            getViewState().hideProgress();
                        }
                    });
        }
    }

    void callBlockUser(boolean isBlocked, String lastMessage, long timestamp) {
        chatInteractor.callBlockUser(lastMessage, timestamp)
                .subscribe(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean success) {
                        getViewState().changeBlockStatus(isBlocked);
                    }

                    @Override
                    public void onError(Throwable e) {
                        getViewState().showError(e);
                        getViewState().hideProgress();
                    }
                });
    }

    private void subscribeCurrentUserData() {
                chatInteractor.subscribeToCurrentUserData()
                    .subscribe(new DisposableObserver<List<Message>>() {
                        @Override
                        public void onNext(List<Message> messages) {
                            getViewState().addMessages(messages);
                        }

                        @Override
                        public void onError(Throwable e) {
                           getViewState().showError(e);
                            getViewState().hideProgress();
                        }

                        @Override
                        public void onComplete() {

                        }
                    });

    }

    private void subscribeOtherUserData() {
        chatInteractor.subscribeToOtherUserData()
                .subscribe(new DisposableObserver<Integer>() {
                    @Override
                    public void onNext(Integer unreadMessageCount) {
                        getViewState().setUnreadCount(unreadMessageCount);
                    }

                    @Override
                    public void onError(Throwable e) {
                        getViewState().showError(e);
                        getViewState().hideProgress();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void subscribeOtherUserOnline() {
        chatInteractor.subscribeToOtherUserOnline()
                .subscribe(new DisposableObserver<Boolean>() {

                    @Override
                    public void onNext(Boolean isOnline) {
                        getViewState().showOtherUserOnline(isOnline);
                    }

                    @Override
                    public void onError(Throwable e) {
                        getViewState().showError(e);
                        getViewState().hideProgress();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void getUserBlockStatus() {
        chatInteractor.getUserBlockStatus()
                .subscribe(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        getViewState().changeBlockStatus(aBoolean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        getViewState().showError(e);
                        getViewState().hideProgress();
                    }
                });
    }

    private void getAnswers() {
        chatInteractor.getAnswers()
                .subscribe(new DisposableSingleObserver<List<String>>() {
                    @Override
                    public void onSuccess(List<String> answers) {
                        getViewState().showAnswer(answers);
                    }

                    @Override
                    public void onError(Throwable e) {
                        getViewState().showError(e);
                        getViewState().hideProgress();
                    }
                });
    }

    private void getMessages() {
        chatInteractor.getChatMessages(0)
                .doOnSubscribe(ob -> getUserBlockStatus())
                .subscribe(new DisposableSingleObserver<List<Message>>() {
                    @Override
                    public void onSuccess(List<Message> messages) {
                        getViewState().addMessages(messages);
                        subscribeToChatData();
                        getViewState().hideProgress();
                    }

                    @Override
                    public void onError(Throwable e) {
                        getViewState().showError(e);
                        getViewState().hideProgress();
                    }

                });
    }


    void updateCurrentUserData(String message, long timestamp) {
        chatInteractor.sendUpdateUser(message, timestamp)
                .subscribe(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        getViewState().showError(e);
                        getViewState().hideProgress();
                    }
                });
    }

}
