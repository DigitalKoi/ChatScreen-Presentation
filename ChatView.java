package service.didi.com.offerdunkan.chat.presentation;

import java.util.List;

import service.didi.com.offerdunkan.base.BaseViewMenu;
import service.didi.com.offerdunkan.chats.domain.model.Message;
import service.didi.com.offerdunkan.chats.domain.model.UserEntity;

public interface ChatView extends BaseViewMenu {

    void addMessages(List<Message> messages);

    void setUnreadCount(Integer unreadMessageCount);

    void showAnswer(List<String> answers);

    void showOtherUserData(UserEntity user);

    void changeBlockStatus(boolean blocked);

    void showOtherUserOnline(boolean isOnline);

    void showMessage(String message);
}
