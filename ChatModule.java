package service.didi.com.offerdunkan.chat.di;


import dagger.Module;
import dagger.Provides;
import service.didi.com.offerdunkan.chat.data.ChatRepo;
import service.didi.com.offerdunkan.chat.data.IChatRepo;
import service.didi.com.offerdunkan.chat.domain.interactor.ChatInteractor;
import service.didi.com.offerdunkan.chat.domain.interactor.IChatInteractor;
import service.didi.com.offerdunkan.dagger.scopes.ChatScope;
import service.didi.com.offerdunkan.network.API;
import service.didi.com.offerdunkan.thirdparty.data.firebase.cloud.CloudRepository;
import service.didi.com.offerdunkan.thirdparty.data.firebase.database.FirebaseDatabaseRepository;
import service.didi.com.offerdunkan.utils.SharedPreferencesUtil;

@Module
public class ChatModule {

    @ChatScope @Provides
    public CloudRepository provideCloudRepository() {
        return new CloudRepository();
    }

    @Provides @ChatScope
    public IChatRepo provideIChatRepo(
            API api,
            SharedPreferencesUtil preferencesUtil,
            FirebaseDatabaseRepository firebaseDatabaseRepository,
            CloudRepository cloudRepository
    ) {
        return new ChatRepo(
                api,
                preferencesUtil,
                firebaseDatabaseRepository,
                cloudRepository);
    }

    @Provides @ChatScope
    public IChatInteractor provideIChatInteractor(IChatRepo chatRepo) {
        return new ChatInteractor(chatRepo);
    }
}
