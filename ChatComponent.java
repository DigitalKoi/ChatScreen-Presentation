package service.didi.com.offerdunkan.chat.di;

import dagger.Subcomponent;
import service.didi.com.offerdunkan.chat.presentation.ChatPresenter;
import service.didi.com.offerdunkan.dagger.scopes.ChatScope;

@ChatScope
@Subcomponent(modules = {ChatModule.class})
public interface ChatComponent {
    void inject(ChatPresenter chatPresenter);
}
