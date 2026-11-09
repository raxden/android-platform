package com.raxdenstudios.platform.device.di.modules

import android.content.pm.ApplicationInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.raxdenstudios.platform.device.DebugInAppUpdate
import com.raxdenstudios.platform.device.InAppUpdate
import com.raxdenstudios.platform.device.InAppUpdateConfig
import com.raxdenstudios.platform.device.InAppUpdateImpl
import com.google.firebase.analytics.FirebaseAnalytics
import com.raxdenstudios.commons.permissions.PermissionsManager
import com.raxdenstudios.commons.permissions.PermissionsManagerImpl
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.device.DebugInAppReview
import com.raxdenstudios.platform.device.InAppReview
import com.raxdenstudios.platform.device.ReleaseInAppReview
import com.raxdenstudios.platform.device.service.mapper.FCMNotificationMapperImpl
import com.raxdenstudios.platform.device.service.FCMMessagingTracker
import com.raxdenstudios.platform.device.service.FCMMessagingTrackerImpl
import com.raxdenstudios.platform.device.data.datasource.NotificationDataSource
import com.raxdenstudios.platform.device.data.datasource.NotificationDataSourceImpl
import com.raxdenstudios.platform.device.data.datasource.NotificationTracker
import com.raxdenstudios.platform.device.data.datasource.NotificationTrackerImpl
import com.raxdenstudios.platform.device.service.mapper.FCMNotificationMapper
import com.raxdenstudios.platform.device.data.FirebaseMessagingProvider
import com.raxdenstudios.platform.device.data.FirebaseMessagingProviderImpl
import com.raxdenstudios.platform.device.data.datasource.LocationDataSource
import com.raxdenstudios.platform.device.data.datasource.LocationDataSourceImpl
import com.raxdenstudios.platform.device.data.datasource.AppAccessLocalDataSource
import com.raxdenstudios.platform.device.data.datasource.AppAccessLocalDataSourceImpl
import com.raxdenstudios.platform.device.data.datasource.FCMLocalDataSource
import com.raxdenstudios.platform.device.data.datasource.FCMLocalDataSourceImpl
import com.raxdenstudios.platform.device.data.mapper.LocationMapper
import com.raxdenstudios.platform.device.data.mapper.LocationMapperImpl
import com.raxdenstudios.platform.device.data.repository.AppRepository
import com.raxdenstudios.platform.device.data.repository.AppRepositoryImpl
import com.raxdenstudios.platform.device.domain.repository.FCMRepository
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
import com.raxdenstudios.platform.device.domain.usecase.SendNotificationUseCase
import com.raxdenstudios.platform.device.domain.usecase.SendNotificationUseCaseImpl
import com.raxdenstudios.platform.device.domain.repository.NotificationRepository
import com.raxdenstudios.platform.device.data.repository.NotificationRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

class DeviceDIDataModule : DIModule {

    override fun get(): Module = module {
        single { FirebaseAnalytics.getInstance(androidContext()) }

        factoryOf(::FCMMessagingTrackerImpl) bind FCMMessagingTracker::class
        factoryOf(::FCMNotificationMapperImpl) bind FCMNotificationMapper::class
        factoryOf(::NotificationTrackerImpl) bind NotificationTracker::class
        factory<NotificationDataSource> { NotificationDataSourceImpl(androidContext(), get()) }
        factoryOf(::NotificationRepositoryImpl) bind NotificationRepository::class
        factoryOf(::SendNotificationUseCaseImpl) bind SendNotificationUseCase::class

        factory<FCMLocalDataSource> { FCMLocalDataSourceImpl(androidContext()) }
        factoryOf(::FirebaseMessagingProviderImpl) bind FirebaseMessagingProvider::class
        factoryOf(::FCMRepositoryImpl) bind FCMRepository::class

        factoryOf(::SubscribeFCMTopicUseCaseImpl) bind SubscribeFCMTopicUseCase::class
        factoryOf(::GetFCMTokenUseCaseImpl) bind GetFCMTokenUseCase::class
        factoryOf(::SaveFCMTokenUseCaseImpl) bind SaveFCMTokenUseCase::class

        single<InAppUpdate> {
            val context = androidContext()
            when {
                context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 ->
                    DebugInAppUpdate()

                else -> InAppUpdateImpl(
                    manager = AppUpdateManagerFactory.create(context),
                    config = getOrNull() ?: InAppUpdateConfig(),
                )
            }
        }

        single<InAppReview> {
            val context = androidContext()
            // BuildConfig.DEBUG can't decide this: it's baked into the library AAR and is always
            // false for consumers. FLAG_DEBUGGABLE reflects the app's build type at runtime.
            when {
                context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 ->
                    DebugInAppReview(context)

                else -> ReleaseInAppReview(context)
            }
        }

        singleOf(::PermissionsManagerImpl) bind PermissionsManager::class
        factoryOf(::LocationMapperImpl) bind LocationMapper::class
        single<LocationDataSource> { LocationDataSourceImpl(androidContext(), get(), get()) }

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
