package service.didi.com.offerdunkan.chat.domain.interactor;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import service.didi.com.offerdunkan.chat.data.IChatRepo;
import service.didi.com.offerdunkan.chats.domain.model.Message;
import service.didi.com.offerdunkan.chats.domain.model.RoomActiveThread;
import service.didi.com.offerdunkan.chats.domain.model.UserBlockInfo;
import service.didi.com.offerdunkan.chats.domain.model.UserFirebase;
import service.didi.com.offerdunkan.chats.domain.model.UserEntity;
import service.didi.com.offerdunkan.saletype.domain.model.ProfileAllowed;

public class ChatInteractor implements IChatInteractor {

    private IChatRepo chatRepo;

    private List<UserEntity> userList = new ArrayList<>();
    private boolean isBlockedCurrentUser;
    private boolean isBlockedOtherUser;
    private int unreadMessageCount = 0;
    private String roomId;

    @Inject
    public ChatInteractor(IChatRepo chatRepo) {
        this.chatRepo = chatRepo;
    }

    @Override
    public Observable<ProfileAllowed> profileAllowed(String uid, String email, String provider) {
        return chatRepo.profileAllowed(uid, email, provider);
    }

    @Override
    public Single<UserEntity> getUsersInfo(String otherUser, String roomId) {
        return chatRepo.getUserById(chatRepo.getMyId(), otherUser)
                .retryWhen(throwable ->
                        throwable.take(5).delay(300, TimeUnit.MILLISECONDS))
                .map(users -> {
                    for (UserEntity user : users) user.setRoomId(roomId);
                    userList.addAll(users);
                    this.roomId = roomId;
                    return users.get(users.get(0).getIsCurrentUser() ? 1 : 0);})
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<Boolean> prepareUsersDataAndRoom() {
        return Single.just(getUserBlockStatus())
                .map(aBoolean -> userList.get(userList.get(0).getIsCurrentUser() ? 1 : 0))
                .retryWhen(throwable ->
                        throwable.take(5).delay(300, TimeUnit.MILLISECONDS))
                .flatMap(user -> chatRepo.getUserDataOnce(user))
                .retryWhen(throwable ->
                        throwable.take(5).delay(300, TimeUnit.MILLISECONDS))
                .map(user -> true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<Boolean> checkBlockedStatusFirebase() {
        return Single.just(userList.get(userList.get(0).getIsCurrentUser() ? 0 : 1).getId())
                .flatMap(userId -> chatRepo.getRoomDataOnce(userId, roomId))
                .map(room -> {
                    room.setBlockedOtherUser(isBlockedOtherUser);
                    return room;
                })
                .flatMap(room -> chatRepo.sendRoomUpdate(new UserFirebase(
                        (userList.get(userList.get(0).getIsCurrentUser() ? 0 : 1).getId()),
                        Collections.singletonList(room))))
                .retryWhen(throwable ->
                        throwable.take(3).delay(300, TimeUnit.MILLISECONDS))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<List<Message>> getChatMessages(int lastMessagesCount) {
        return Single.just(roomId)
                .doOnSuccess(roomId -> chatRepo.setRoomId(roomId))
                .flatMap(roomPath -> chatRepo.getMessagesOnce(roomPath, lastMessagesCount).firstOrError())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<Message>> subscribeToCurrentUserData() {
        return Single.just(userList.get(userList.get(0).getIsCurrentUser() ? 0 : 1))
                .toObservable()
                .flatMap(user -> chatRepo.subscribeCurrentRoomEvent(user.getId(), roomId))
                .skip(1)
                .filter(roomActiveThread -> roomActiveThread.getUnreadMessageCount() > 0)
                .flatMap(roomActiveThread -> chatRepo.getMessagesOnce(roomId, roomActiveThread.getUnreadMessageCount()))
                .onErrorResumeNext(Observable.empty())
                .distinctUntilChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Integer> subscribeToOtherUserData() {
        return chatRepo.subscribeOtherUserEvent(
                        userList.get(userList.get(0).getIsCurrentUser() ? 1 : 0).getId(),
                        roomId)
                .map(roomActiveThread -> {
                        isBlockedCurrentUser = roomActiveThread.getBlockedOtherUser();
                        return unreadMessageCount = roomActiveThread.getUnreadMessageCount();
                    })
                .retryWhen(throwableObservable -> throwableObservable.take(20).delay(5, TimeUnit.SECONDS))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<List<String>> getAnswers() {
        return chatRepo.getChatAnswer()
                .map(answers -> Arrays.asList(answers.getAnswer()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<List<Message>> sendMessage(String text) {
        List<Message> messageList = new ArrayList<>(1);

        return Single.just(generateMessage(text, userList))
                .doOnSuccess(message -> messageList.add(0, message))
                .flatMap(message -> chatRepo.sendMessage(message))
                .flatMap(users -> sendNotification(userList, messageList.get(0).getRoom(), text))
                .map(aBoolean ->
                        generateUsersFirebaseData(userList, messageList.get(0)))
                .flatMap(userFirebase -> chatRepo.sendUsersUpdate(userFirebase))
                .map(ob -> messageList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<List<Message>> sendPhoto(ArrayList<String> photos) {
        List<Message> messageList = new ArrayList<>(1);

        return Single.just(generateMessagePhoto(photos, userList))
                .doOnSuccess(messageList::add)
                .flatMap(message -> chatRepo.sendPhoto(
                        Uri.fromFile(
                                new File(message.getPhotoUrl())),
                                String.valueOf(message.getTimestamp())
                ))
                .map(uri -> {
                    messageList.get(0).setPhotoUrl(uri.toString());
                    return messageList.get(0);
                })
                .flatMap(message -> sendNotification(userList, message.getRoom(), "Photo"))
                .flatMap(object -> chatRepo.sendMessage(messageList.get(0)))
                .map(aBoolean -> generateUsersFirebaseData(userList, messageList.get(0)))
                .flatMap(userFirebase -> chatRepo.sendUsersUpdate(userFirebase))
                .map(ob -> messageList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<Boolean> getUserBlockStatus() {
        return chatRepo.getBlockedList(chatRepo.getMyId())
                .map(blockResponse -> {
                    boolean isOtherUser = !userList.get(0).getIsCurrentUser();
                    String otherUserId = userList.get(isOtherUser ? 0 : 1).getId();
                    for (UserBlockInfo.UserBlocked userBlocked : blockResponse.getListBlocked()) {
                        if (TextUtils.equals(otherUserId, userBlocked.getUserId())) {
                            return isBlockedOtherUser = true;
                        }
                    }
                    return isBlockedOtherUser = false;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<Boolean> callBlockUser(String lastMessage, long timestamp) {
        return getUserBlockStatus()
                .map(status -> {
                    boolean currentUserIsFirst = userList.get(0).getIsCurrentUser();
                    return new String[] {
                            userList.get(currentUserIsFirst ? 0 : 1).getId(),
                            userList.get(currentUserIsFirst ? 1 : 0).getId(),
                            status ? "unblock" : "block"
                    };
                })
                .flatMap(strings -> chatRepo.callBlockUser(strings[0], strings[1], strings[2]))
                .map(ob ->
                        isBlockedOtherUser = !isBlockedOtherUser)
                .flatMap(isBlocked -> sendUpdateUser(lastMessage, timestamp))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Boolean> subscribeToOtherUserOnline() {
        return Single.just(userList.get(userList.get(0).getIsCurrentUser() ? 1 : 0).getId())
                .toObservable()
                .flatMap(userId -> chatRepo.subscribeToOtherUserOnline(userId))
                .onErrorResumeNext(Observable.empty())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Single<Boolean> sendUpdateUser(String message, long timestamp) {
        return Single.just(generateUserFirebaseData(userList, message))
                .flatMap(user -> chatRepo.sendRoomUpdate(user))
                .doOnError(throwable -> Log.d("FindError", "sendUpdateUser: "))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Single<Object> sendNotification(List<UserEntity> users, String roomId, String text) {
        boolean currentUserIsFirst = users.get(0).getIsCurrentUser();
        String id = users.get(currentUserIsFirst ? 1 : 0).getId();
        String name = users.get(currentUserIsFirst ? 0 : 1).getName();
        return chatRepo.sendNotification(id, name, roomId, text);
    }

    private Message generateMessage(String message, List<UserEntity> users) {
        Message newMessage = new Message();
        String currentUserID = users.get(users.get(0).getIsCurrentUser() ? 0 : 1).getId();

        newMessage.setText(message);
        for (UserEntity user : users) {
            if (user.getIsCurrentUser()) {
                newMessage.setSenderId(currentUserID);
                newMessage.setSenderName(user.getName());
                newMessage.setSenderPhoto(user.getPhoto());
            }
        }

        newMessage.setTimestamp(System.currentTimeMillis());
        newMessage.setRoom(roomId);

        return newMessage;
    }

    private UserFirebase generateUserFirebaseData(List<UserEntity> userEntity, String message) {
        long timestamp = System.currentTimeMillis();
        boolean firstIsCurrentUser = userEntity.get(0).getIsCurrentUser();
        int currentUserIndex = firstIsCurrentUser ? 0 : 1;
        int otherUserIndex = firstIsCurrentUser ? 1 : 0;
        RoomActiveThread currentUserRoom = new RoomActiveThread(
                roomId,
                message,
                userEntity.get(otherUserIndex).getId(),
                0,
                timestamp,
                isBlockedOtherUser
        );
        return new UserFirebase(
                userEntity.get(currentUserIndex).getId(),
                new ArrayList<>(Collections.singletonList(currentUserRoom)) );
    }

    private List<UserFirebase> generateUsersFirebaseData(List<UserEntity> usersFromDatabase, Message message) {
        long timestamp = System.currentTimeMillis();
        unreadMessageCount++;
        boolean firstIsCurrentUser = usersFromDatabase.get(0).getIsCurrentUser();
        int currentUserIndex = firstIsCurrentUser ? 0 : 1;
        int otherUserIndex = firstIsCurrentUser ? 1 : 0;

        RoomActiveThread currentUserRoom = new RoomActiveThread(
                roomId,
                TextUtils.isEmpty(message.getText()) ? "Photo" : message.getText(),
                usersFromDatabase.get(otherUserIndex).getId(),
                0,
                timestamp,
                isBlockedOtherUser
        );

        RoomActiveThread otherUserRoom = new RoomActiveThread(
                roomId,
                TextUtils.isEmpty(message.getText()) ? "Photo" : message.getText(),
                usersFromDatabase.get(currentUserIndex).getId(),
                unreadMessageCount,
                timestamp,
                isBlockedCurrentUser
        );

        UserFirebase currentUserFirebase = new UserFirebase(
                usersFromDatabase.get(currentUserIndex).getId(),
                new ArrayList<>(Collections.singletonList(currentUserRoom)) );

        UserFirebase otherUserFirebase = new UserFirebase(
                usersFromDatabase.get(otherUserIndex).getId(),
                new ArrayList<>(Collections.singletonList(otherUserRoom)));

        return new ArrayList<>(Arrays.asList(currentUserFirebase, otherUserFirebase));
    }

    private Message generateMessagePhoto(ArrayList<String> urlPhotos, List<UserEntity> users) {
        Message newMessage = new Message();
        String currentUserID = users.get(users.get(0).getIsCurrentUser() ? 0 : 1).getId();

        for (UserEntity user : users) {
            if (user.getIsCurrentUser()) {
                newMessage.setSenderId(currentUserID);
                newMessage.setSenderName(user.getName());
                newMessage.setSenderPhoto(user.getPhoto());
            }
        }
        newMessage.setTimestamp(System.currentTimeMillis());
        newMessage.setRoom(roomId);
        newMessage.setPhotoUrl(urlPhotos.get(0));
        newMessage.setText("Photo");

        return newMessage;
    }

    @Override
    public Boolean isBlockedCurrentUser() {
        return isBlockedCurrentUser;
    }

    @Override
    public void unsubscribeListeners() {
        chatRepo.unsubscribeFirebaseListeners();
    }
}
