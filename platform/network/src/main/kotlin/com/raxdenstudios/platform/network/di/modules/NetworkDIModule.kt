package com.raxdenstudios.platform.network.di.modules

import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.raxdenstudios.platform.core.di.DIModule
import com.raxdenstudios.platform.network.HttpClientFactory
import com.raxdenstudios.platform.network.HttpClientFactoryImpl
import com.raxdenstudios.commons.android.util.Network
import com.raxdenstudios.commons.network.interceptor.CacheLoggerInterceptor
import com.raxdenstudios.commons.network.interceptor.CacheNetworkInterceptor
import com.raxdenstudios.commons.network.interceptor.CacheOfflineInterceptor
import com.raxdenstudios.commons.network.interceptor.RetryInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

internal class NetworkDIModule : DIModule {

    override fun get(): Module = module {
        single {
            Cache(File(androidContext().cacheDir, CACHE_DIRECTORY), CACHE_SIZE.toLong())
        }
        single { RetryInterceptor.default }
        single { HttpLoggingInterceptor.Logger { message -> Timber.tag("OkHttp").d(message) } }
        single<HttpLoggingInterceptor> {
            HttpLoggingInterceptor(get<HttpLoggingInterceptor.Logger>()).apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
        }
        single {
            ChuckerInterceptor.Builder(androidContext())
                .collector(ChuckerCollector(
                    context = androidContext(),
                    showNotification = true,
                    retentionPeriod = RetentionManager.Period.ONE_HOUR
                ))
                .alwaysReadResponseBody(true)
                .build()
        }
        single { CacheOfflineInterceptor.default { Network.isNetworkConnected(androidContext()) } }
        single { CacheNetworkInterceptor.default }
        single { CacheLoggerInterceptor { message -> Timber.tag("OkHttp-Cache").d(message) } }

        factory<SSLSocketFactory?> { null }
        factory<X509TrustManager?> { null }

        singleOf(::HttpClientFactoryImpl) bind HttpClientFactory::class
        single<OkHttpClient> {
            get<HttpClientFactory>().create()
        }

        single<Gson> {
            GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create()
        }

        single<Retrofit.Builder> {
            Retrofit.Builder()
                .client(get<OkHttpClient>())
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(get<Gson>()))
        }
    }

    private companion object {
        const val CACHE_DIRECTORY = "responses"
        const val CACHE_SIZE = 50 * 1024 * 1024  // 50 MiB
    }
}
