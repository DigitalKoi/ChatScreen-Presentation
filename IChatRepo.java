package service.didi.com.offerdunkan.chat.data;

import android.net.Uri;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import service.didi.com.offerdunkan.chat.domain.model.Answer;
import service.didi.com.offerdunkan.chats.domain.model.UserBlockInfo;
import service.didi.com.offerdunkan.chats.domain.model.Message;
import service.didi.com.offerdunkan.chats.domain.model.RoomActiveThread;
import service.didi.com.offerdunkan.chats.domain.model.UserFirebase;
import service.didi.com.offerdunkan.chats.domain.model.UserFirebaseV2;
import service.didi.com.offerdunkan.chats.domain.model.UserEntity;
import service.didi.com.offerdunkan.saletype.domain.model.ProfileAllowed;

public interface IChatRepo {

    // region Base methods
    Observable<ProfileAllowed> profileAllowed(String uid, String email, String provider);

    Observable<List<Message>> getMessagesOnce(String roomId, int lastMessagesCount);
    // endregion

    // region SharedPref
    String getMyId();

    void setRoomId(String roomId);
    // endregion

    // region Api server
    Single<Uri> sendPhoto(Uri file, String nameFile);

    Single<List<UserEntity>> getUserById(String currentUserId, String  otherUserId);

    Single<Object> sendNotification(String receiverUserId, String senderUserName, String roomId, String message);

    Single<UserBlockInfo> getBlockedList(String currentUserId);

    Single<Boolean> callBlockUser(String currentUserId, String otherUserId, String newStatus);

    Single<Answer> getChatAnswer();
    // endregion

    // region Firebase
    Observable<RoomActiveThread> subscribeOtherUserEvent(String userId, String roomId);

    Observable<RoomActiveThread> subscribeCurrentRoomEvent(String userId, String roomId);

    Single<Boolean> sendMessage(Message newMessage);

    Single<UserFirebaseV2> getUserDataOnce(UserEntity user);

    Single<Boolean> sendUsersUpdate(List<UserFirebase> userFirebaseList);

    Single<Boolean> sendRoomUpdate(UserFirebase userFirebase);

    Observable<Boolean> subscribeToOtherUserOnline(String userId);

    void unsubscribeFirebaseListeners();

    Single<RoomActiveThread> getRoomDataOnce(String userId, String roomId);
    // endregion
}
