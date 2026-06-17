package com.raxdenstudios.platform.device.di.modules

import com.google.firebase.analytics.FirebaseAnalytics
import com.raxdenstudios.commons.permissions.PermissionsManager
import com.raxdenstudios.commons.permissions.PermissionsManagerImpl
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.device.DefaultPushNotificationProcessor
import com.raxdenstudios.platform.device.DeviceMessagingTracker
import com.raxdenstudios.platform.device.DeviceMessagingTrackerImpl
import com.raxdenstudios.platform.device.NotificationSender
import com.raxdenstudios.platform.device.NotificationSenderImpl
import com.raxdenstudios.platform.device.NotificationTracker
import com.raxdenstudios.platform.device.NotificationTrackerImpl
import com.raxdenstudios.platform.device.PushNotificationProcessor
import com.raxdenstudios.platform.device.data.FirebaseMessagingProvider
import com.raxdenstudios.platform.device.data.FirebaseMessagingProviderImpl
import com.raxdenstudios.platform.device.data.LocationManager
import com.raxdenstudios.platform.device.data.LocationManagerImpl
import com.raxdenstudios.platform.device.data.datasource.AppAccessLocalDataSource
import com.raxdenstudios.platform.device.data.datasource.AppAccessLocalDataSourceImpl
import com.raxdenstudios.platform.device.data.datasource.FCMLocalDataSource
import com.raxdenstudios.platform.device.data.datasource.FCMLocalDataSourceImpl
import com.raxdenstudios.platform.device.data.repository.AppRepository
import com.raxdenstudios.platform.device.data.repository.AppRepositoryImpl
import com.raxdenstudios.platform.device.data.repository.FCMRepository
import com.raxdenstudios.platform.device.data.repository.FCMRepositoryImpl
import com.raxdenstudios.platform.device.data.repository.LocationRepository
import com.raxdenstudios.platform.device.data.repository.LocationRepositoryImpl
import com.raxdenstudios.platform.device.data.repository.PermissionRepository
import com.raxdenstudios.platform.device.data.repository.PermissionRepositoryImpl
import com.raxdenstudios.platform.device.domain.usecase.GetAppVersionUseCase
import com.raxdenstudios.platform.device.domain.usecase.GetAppVersionUseCaseImpl
import com.raxdenstudios.platform.device.domain.usecase.GetFCMTokenUseCase
import com.raxdenstudios.platform.device.domain.usecase.GetFCMTokenUseCaseImpl
import com.raxdenstudios.platform.device.domain.usecase.GetLastAccessUseCase
import com.raxdenstudios.platform.device.domain.usecase.GetLastAccessUseCaseImpl
import com.raxdenstudios.platform.device.domain.usecase.GetLastLocationUseCase
import com.raxdenstudios.platform.device.domain.usecase.GetLastLocationUseCaseImpl
import com.raxdenstudios.platform.device.domain.usecase.HasPermissionUseCase
import com.raxdenstudios.platform.device.domain.usecase.HasPermissionUseCaseImpl
import com.raxdenstudios.platform.device.domain.usecase.RequestPermissionUseCase
import com.raxdenstudios.platform.device.domain.usecase.RequestPermissionUseCaseImpl
import com.raxdenstudios.platform.device.domain.usecase.SaveFCMTokenUseCase
import com.raxdenstudios.platform.device.domain.usecase.SaveFCMTokenUseCaseImpl
import com.raxdenstudios.platform.device.domain.usecase.SubscribeFCMTopicUseCase
import com.raxdenstudios.platform.device.domain.usecase.SubscribeFCMTopicUseCaseImpl
import com.raxdenstudios.platform.device.domain.usecase.UpdateLastAccessUseCase
import com.raxdenstudios.platform.device.domain.usecase.UpdateLastAccessUseCaseImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

class DeviceDIDataModule : DIModule {

    override fun get(): Module = module {
        single { FirebaseAnalytics.getInstance(androidContext()) }

        factoryOf(::DeviceMessagingTrackerImpl) bind DeviceMessagingTracker::class
        factoryOf(::DefaultPushNotificationProcessor) bind PushNotificationProcessor::class
        factoryOf(::NotificationTrackerImpl) bind NotificationTracker::class
        factoryOf(::NotificationSenderImpl) bind NotificationSender::class

        factory<FCMLocalDataSource> { FCMLocalDataSourceImpl(androidContext()) }
        factoryOf(::FirebaseMessagingProviderImpl) bind FirebaseMessagingProvider::class
        factoryOf(::FCMRepositoryImpl) bind FCMRepository::class

        factoryOf(::SubscribeFCMTopicUseCaseImpl) bind SubscribeFCMTopicUseCase::class
        factoryOf(::GetFCMTokenUseCaseImpl) bind GetFCMTokenUseCase::class
        factoryOf(::SaveFCMTokenUseCaseImpl) bind SaveFCMTokenUseCase::class

        singleOf(::PermissionsManagerImpl) bind PermissionsManager::class
        singleOf(::LocationManagerImpl) bind LocationManager::class

        singleOf(::AppAccessLocalDataSourceImpl) bind AppAccessLocalDataSource::class
        factoryOf(::AppRepositoryImpl) bind AppRepository::class
        factoryOf(::GetLastAccessUseCaseImpl) bind GetLastAccessUseCase::class
        factoryOf(::UpdateLastAccessUseCaseImpl) bind UpdateLastAccessUseCase::class

        factoryOf(::GetAppVersionUseCaseImpl) bind GetAppVersionUseCase::class

        factoryOf(::PermissionRepositoryImpl) bind PermissionRepository::class
        factoryOf(::LocationRepositoryImpl) bind LocationRepository::class
        factoryOf(::GetLastLocationUseCaseImpl) bind GetLastLocationUseCase::class
        factoryOf(::HasPermissionUseCaseImpl) bind HasPermissionUseCase::class
        factoryOf(::RequestPermissionUseCaseImpl) bind RequestPermissionUseCase::class
    }
}
