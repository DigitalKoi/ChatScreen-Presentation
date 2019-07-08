package service.didi.com.offerdunkan.chat.domain.interactor;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import service.didi.com.offerdunkan.base.BaseInteractorMenu;
import service.didi.com.offerdunkan.chats.domain.model.Message;
import service.didi.com.offerdunkan.chats.domain.model.UserEntity;
import service.didi.com.offerdunkan.saletype.domain.model.ProfileAllowed;

public interface IChatInteractor extends BaseInteractorMenu {

    Observable<ProfileAllowed> profileAllowed(String uid, String email, String provider);

    Observable<List<Message>> subscribeToCurrentUserData();

    Observable<Integer> subscribeToOtherUserData();

    Single<List<Message>> getChatMessages(int lastMessagesCount);

    Single<List<String>> getAnswers();

    Single<List<Message>> sendMessage(String message);

    Single<List<Message>> sendPhoto(ArrayList<String> photos);

    Single<UserEntity> getUsersInfo(String otherUser, String roomId);

    Single<Boolean> getUserBlockStatus();

    Single<Boolean> callBlockUser(String lastMessage, long timestamp);

    Observable<Boolean> subscribeToOtherUserOnline();

    Single<Boolean> sendUpdateUser(String message, long timestamp);

    Boolean isBlockedCurrentUser();

    Single<Boolean> prepareUsersDataAndRoom();

    Single<Boolean> checkBlockedStatusFirebase();

    void unsubscribeListeners();
}
