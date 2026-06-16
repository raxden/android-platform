package com.raxdenstudios.platform.network

import com.raxdenstudios.commons.network.interceptor.CacheLoggerInterceptor
import com.raxdenstudios.commons.network.interceptor.CacheNetworkInterceptor
import com.raxdenstudios.commons.network.interceptor.CacheOfflineInterceptor
import com.raxdenstudios.commons.network.interceptor.RetryInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class HttpClientFactoryImpl(
    private val retryInterceptor: RetryInterceptor,
//    private val chuckerInterceptor: ChuckerInterceptor,
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    private val cacheOfflineInterceptor: CacheOfflineInterceptor,
    private val cacheNetworkInterceptor: CacheNetworkInterceptor,
    private val cacheLoggerInterceptor: CacheLoggerInterceptor,
    private val cache: Cache,
) : HttpClientFactory {

    override fun create(): OkHttpClient = initOkHttpClientBuilder()
        .build()

    override fun create(
        sslSocketFactory: SSLSocketFactory,
        x509TrustManager: X509TrustManager
    ): OkHttpClient = initOkHttpClientBuilder()
        .sslSocketFactory(sslSocketFactory, x509TrustManager)
        .build()

    private fun initOkHttpClientBuilder() = OkHttpClient.Builder()
        .cache(cache)
        // Application Interceptors (executed before cache check)
        .addInterceptor(retryInterceptor)
//        .addInterceptor(chuckerInterceptor) Requires notification permission
        .addInterceptor(httpLoggingInterceptor)
        .addInterceptor(cacheOfflineInterceptor)
        // Network Interceptors (executed only on network calls)
        .addNetworkInterceptor(cacheNetworkInterceptor)
        .addNetworkInterceptor(cacheLoggerInterceptor)
        .retryOnConnectionFailure(true)
        .readTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
        .connectTimeout(CONNECTION_TIMEOUT.toLong(), TimeUnit.SECONDS)

    companion object {

        private const val TIMEOUT = 35                  // 35 sec
        private const val CONNECTION_TIMEOUT = 15        // 15 sec
    }
}
