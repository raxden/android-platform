package com.raxdenstudios.platform.notification.di.module

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.notification.NotificationStorage
import com.raxdenstudios.platform.notification.data.datasource.NotificationPermissionLocalDataSource
import com.raxdenstudios.platform.notification.data.datasource.NotificationPermissionLocalDataSourceImpl
import com.raxdenstudios.platform.notification.data.datasource.NotificationTopicLocalDataSource
import com.raxdenstudios.platform.notification.data.datasource.NotificationTopicLocalDataSourceImpl
import com.raxdenstudios.platform.notification.data.datasource.NotificationTopicRemoteDataSource
import com.raxdenstudios.platform.notification.data.datasource.NotificationTopicRemoteDataSourceImpl
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepository
import com.raxdenstudios.platform.notification.data.repository.NotificationPermissionRepositoryImpl
import com.raxdenstudios.platform.notification.data.repository.NotificationTopicRepository
import com.raxdenstudios.platform.notification.data.repository.NotificationTopicRepositoryImpl
import com.raxdenstudios.platform.notification.domain.AreNotificationsEnabledUseCase
import com.raxdenstudios.platform.notification.domain.AreNotificationsEnabledUseCaseImpl
import com.raxdenstudios.platform.notification.domain.GetNotificationPermissionStateUseCase
import com.raxdenstudios.platform.notification.domain.GetNotificationPermissionStateUseCaseImpl
import com.raxdenstudios.platform.notification.domain.LaunchNotificationPermissionDialogUseCase
import com.raxdenstudios.platform.notification.domain.LaunchNotificationPermissionDialogUseCaseImpl
import com.raxdenstudios.platform.notification.domain.NotificationTopicLock
import com.raxdenstudios.platform.notification.domain.NotificationTopicLockImpl
import com.raxdenstudios.platform.notification.domain.SyncNotificationTopicUseCase
import com.raxdenstudios.platform.notification.domain.SyncNotificationTopicUseCaseImpl
import com.raxdenstudios.platform.notification.domain.WasNotificationPermissionRequestedUseCase
import com.raxdenstudios.platform.notification.domain.WasNotificationPermissionRequestedUseCaseImpl
import com.raxdenstudios.platform.notification.permission.NotificationPermissionLauncher
import com.raxdenstudios.platform.notification.permission.NotificationPermissionLauncherImpl
import com.raxdenstudios.platform.notification.topic.NotificationTopicSyncer
import com.raxdenstudios.platform.notification.topic.NotificationTopicSyncerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.time.Clock

/**
 * Every flag is persisted in the module's own `SharedPreferences` file ([NotificationStorage.PREFERENCES_NAME]),
 * bound under a private qualifier so it never clashes with the app's default preferences. `FirebaseMessaging` is not
 * bound in Koin either. `NotificationTopicPolicy` and `NotificationTopicSyncListener` are bound by the app, if at all.
 */
internal class NotificationDIModule : DIModule {

    override fun get(): Module = module {
        single(PREFERENCES) { createSharedPreferences(androidContext()) }
        notificationPermission()
        notificationTopic()
    }

    // The launcher is a single instance: it keeps the attached activities and the dialog in progress.
    private fun Module.notificationPermission() {
        singleOf(::NotificationPermissionLauncherImpl) bind NotificationPermissionLauncher::class
        factory<NotificationPermissionLocalDataSource> {
            NotificationPermissionLocalDataSourceImpl(sharedPreferences = get(PREFERENCES))
        }
        factoryOf(::NotificationPermissionRepositoryImpl) bind NotificationPermissionRepository::class
        factoryOf(::GetNotificationPermissionStateUseCaseImpl) bind GetNotificationPermissionStateUseCase::class
        factoryOf(::AreNotificationsEnabledUseCaseImpl) bind AreNotificationsEnabledUseCase::class
        factoryOf(::LaunchNotificationPermissionDialogUseCaseImpl) bind LaunchNotificationPermissionDialogUseCase::class
        factoryOf(::WasNotificationPermissionRequestedUseCaseImpl) bind WasNotificationPermissionRequestedUseCase::class
    }

    // The lock is a single instance shared by every sync. The use case reads the default zone on every sync, so a sync
    // after a time zone change sees the new zone. FirebaseMessaging is only obtained on the first FCM call.
    private fun Module.notificationTopic() {
        singleOf(::NotificationTopicLockImpl) bind NotificationTopicLock::class
        factory<NotificationTopicRemoteDataSource> {
            NotificationTopicRemoteDataSourceImpl(firebaseMessagingProvider = FirebaseMessaging::getInstance)
        }
        factory<NotificationTopicLocalDataSource> {
            NotificationTopicLocalDataSourceImpl(sharedPreferences = get(PREFERENCES))
        }
        factoryOf(::NotificationTopicRepositoryImpl) bind NotificationTopicRepository::class
        factory<SyncNotificationTopicUseCase> {
            SyncNotificationTopicUseCaseImpl(
                repository = get(),
                lock = get(),
                policy = getOrNull(),
                clockProvider = Clock::systemDefaultZone,
            )
        }
        factory<NotificationTopicSyncer> {
            NotificationTopicSyncerImpl(
                syncNotificationTopicUseCase = get(),
                policy = getOrNull(),
                listener = getOrNull(),
            )
        }
    }

    private fun createSharedPreferences(
        context: Context,
    ): SharedPreferences = context.getSharedPreferences(NotificationStorage.PREFERENCES_NAME, Context.MODE_PRIVATE)

    private companion object {
        val PREFERENCES = named("platform_notification_preferences")
    }
}
