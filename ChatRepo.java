    package service.didi.com.offerdunkan.chat.data;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import service.didi.com.offerdunkan.chat.domain.model.Answer;
import service.didi.com.offerdunkan.chats.domain.model.UserBlockInfo;
import service.didi.com.offerdunkan.chats.domain.model.Message;
import service.didi.com.offerdunkan.chats.domain.model.RoomActiveThread;
import service.didi.com.offerdunkan.chats.domain.model.UserFirebase;
import service.didi.com.offerdunkan.chats.domain.model.UserFirebaseV2;
import service.didi.com.offerdunkan.chats.domain.model.UserEntity;
import service.didi.com.offerdunkan.network.API;
import service.didi.com.offerdunkan.saletype.domain.model.ProfileAllowed;
import service.didi.com.offerdunkan.thirdparty.data.firebase.cloud.CloudRepository;
import service.didi.com.offerdunkan.thirdparty.data.firebase.cloud.CloudStorageRepository;
import service.didi.com.offerdunkan.thirdparty.data.firebase.database.FirebaseDatabaseRepository;
import service.didi.com.offerdunkan.utils.SharedPreferencesUtil;

public class ChatRepo implements IChatRepo {

    private FirebaseDatabaseRepository  firebaseDatabaseRepository;
    private SharedPreferencesUtil       preferencesUtil;
    private CloudRepository             cloudRepository;
    private API                         api;


    @Inject
    public ChatRepo(
            API api,
            SharedPreferencesUtil preferencesUtil,
            FirebaseDatabaseRepository firebaseDatabaseRepository,
            CloudRepository cloudRepository) {
        this.firebaseDatabaseRepository = firebaseDatabaseRepository;
        this.cloudRepository = cloudRepository;
        this.preferencesUtil = preferencesUtil;
        this.api = api;
    }

    @Override
    public String getMyId() {
        return preferencesUtil.getUserId();
    }

    @Override
    public void setRoomId(String roomId) {
        preferencesUtil.setRoomId(roomId);
    }

    @Override
    public Observable<ProfileAllowed> profileAllowed(String uid, String email, String provider) {
        return api.profileAllowed(uid, email, provider);
    }

    @Override
    public Observable<List<Message>> getMessagesOnce(String roomId, int lastMessagesCount) {
        return Observable.create(emitter -> {
            if (emitter == null) return;
            firebaseDatabaseRepository.getMessageOnce(roomId, lastMessagesCount,
                    new FirebaseDatabaseRepository.FirebaseDatabaseMessageCallback<Message>() {
                        @Override
                        public void onSuccess(List<Message> result) {
                            emitter.onNext(result);
                        }

                        @Override
                        public void onError(Exception e) {
                        }
                    });
        });
    }

    @Override
    public Single<List<UserEntity>> getUserById(String currentUserId, String  otherUserId) {
        return api.getUserInfoById(currentUserId, otherUserId)
                .map(user -> {
                    UserEntity currentUser = new UserEntity(currentUserId, user.getCurrentUserName(), user.getCurrentUserPhoto(), "", true);
                    UserEntity otherUser = new UserEntity(otherUserId, user.getOtherUserName(), user.getOtherUserPhoto(), "", false);
                    return Arrays.asList(currentUser, otherUser);
                });
    }

    @Override
    public Single<Object> sendNotification(String receiverUserId, String senderUserName, String roomId, String message) {
        return api.sendChatAlert(receiverUserId, senderUserName, roomId, message);
    }

    @Override
    public Single<UserBlockInfo> getBlockedList(String currentUserId) {
        return api.getBlockedList(currentUserId);
    }

    @Override
    public Single<Boolean> callBlockUser(String currentUserId, String otherUserId, String newStatus) {
        return api.callBlockUnblockUser(currentUserId, otherUserId, newStatus);
    }

    @Override
    public Single<Answer> getChatAnswer() {
        return api.getChatAnswer();
    }

    // region Firebase
    @Override
    public Observable<RoomActiveThread> subscribeOtherUserEvent(String userId, String roomId) {
        return Observable.create(emitter -> {
            if (emitter == null) return;
            firebaseDatabaseRepository.addOtherUserRoomListener(userId, roomId,
                    new FirebaseDatabaseRepository.FirebaseDatabaseUserCallback<RoomActiveThread>() {
                        @Override
                        public void onSuccess(RoomActiveThread result) {
//                            if (result == null)
//                                emitter.onNext(new RoomActiveThread());
//                            else
                                emitter.onNext(result);
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
        });
    }

    @Override
    public Observable<RoomActiveThread> subscribeCurrentRoomEvent(String userId, String roomId) {
        return Observable.create(emitter -> {
            if (emitter == null) return;
            firebaseDatabaseRepository.addCurrentUserRoomListener(userId, roomId,
                    new FirebaseDatabaseRepository.FirebaseDatabaseUserCallback<RoomActiveThread>() {
                        @Override
                        public void onSuccess(RoomActiveThread result) {
                            if (result == null) return;
                            emitter.onNext(result);
                        }

                        @Override
                        public void onError(Exception e) {
                        }
                    });
        });
    }

    @Override
    public Single<UserFirebaseV2> getUserDataOnce(UserEntity user) {
        return Single.create(emitter -> {
            if (emitter == null) return;
            firebaseDatabaseRepository.getUserDataOnce(user.getId(),
                    new FirebaseDatabaseRepository.FirebaseDatabaseUserCallback<UserFirebaseV2>() {
                        @Override
                        public void onSuccess(UserFirebaseV2 result) {
                            if (result == null) {
                                result = new UserFirebaseV2(user.getId(), user.getName(), user.getPhoto(), false);
                                firebaseDatabaseRepository.sendOnlineStatus(user.getId(), result);
                            }
                            emitter.onSuccess(result);
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
        });
    }

    @Override
    public Single<Boolean> sendMessage(Message newMessage) {
        return Single.create(emitter -> {
            if (emitter == null) return;
            boolean isCompleted = firebaseDatabaseRepository.sendMessage(newMessage);
            emitter.onSuccess(true);
//            else emitter.onError(
//                    new RuntimeException("Message has not been sent!")
//            );
        });
    }

    @Override
    public Single<Boolean> sendUsersUpdate(List<UserFirebase> userFirebaseList) {
        return Single.create(emitter -> {
            if (emitter == null) return;
            Boolean usersUpdated = firebaseDatabaseRepository.sendUsersRoomUpdate(userFirebaseList);
//            if (usersUpdated)
            emitter.onSuccess(true);
//            else emitter.onError(new RuntimeException("Error updated users"));
        });
    }

    @Override
    public Single<Boolean> sendRoomUpdate(UserFirebase userFirebase) {
        return Single.create(emitter -> {
            if (emitter == null) return;
            Boolean userUpdated = firebaseDatabaseRepository.sendUserRoomUpdate(userFirebase);
            emitter.onSuccess(userUpdated);
        });
    }

    @Override
    public Observable<Boolean> subscribeToOtherUserOnline(String userId) {
        return Observable.create(emitter -> {
            if (emitter == null) return;
            firebaseDatabaseRepository.addOtherUserOnlineListener(userId,
                    new FirebaseDatabaseRepository.FirebaseDatabaseUserCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            emitter.onNext(result);
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
        });
    }

    @Override
    public Single<RoomActiveThread> getRoomDataOnce(String userId, String roomId) {
        return Single.create(emitter -> {
            if (emitter == null) return;
            firebaseDatabaseRepository.getRoomDataOnce(userId, roomId,
                    new FirebaseDatabaseRepository.FirebaseDatabaseUserCallback<RoomActiveThread>() {
                        @Override
                        public void onSuccess(RoomActiveThread result) {
                            emitter.onSuccess(result);
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
        });
    }

    @Override
    public void unsubscribeFirebaseListeners() {
        preferencesUtil.setRoomId("");
        firebaseDatabaseRepository.removeListener();
    }

    @Override
    public Single<Uri> sendPhoto(Uri file, String nameFile) {
        return Single.create(emitter -> {
            if (emitter == null) return;
            cloudRepository.uploadImage(file, nameFile, new CloudStorageRepository.CloudStorageRepositoryCallback() {
                @Override
                public void onSuccess(Uri result) {
                    emitter.onSuccess(result);
                }

                @Override
                public void onError(Exception e) {

                }
            });
        });
    }
    // endregion
}
