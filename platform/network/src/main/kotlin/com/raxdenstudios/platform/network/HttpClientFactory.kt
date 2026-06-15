package com.raxdenstudios.platform.network

import okhttp3.OkHttpClient
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

interface HttpClientFactory {
    fun create(): OkHttpClient

    fun create(
        sslSocketFactory: SSLSocketFactory,
        x509TrustManager: X509TrustManager,
    ): OkHttpClient
}
