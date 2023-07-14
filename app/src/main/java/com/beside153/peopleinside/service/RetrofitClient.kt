package com.beside153.peopleinside.service

import com.beside153.peopleinside.App
import com.beside153.peopleinside.model.common.ErrorEnvelope
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.skydoves.sandwich.adapters.ApiResponseCallAdapterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import timber.log.Timber
import java.io.IOException
import java.net.UnknownHostException

object RetrofitClient {
    private const val baseUrl = "https://people-inside.com"
    private const val contentType = "application/json"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Suppress("ForbiddenComment")
    private val signUpRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        // TODO: 이거 왜 안되지?
        // .client(provideOkHttpClient(listOf(ErrorInterceptor)))
        .addConverterFactory(json.asConverterFactory(contentType.toMediaType()))
        .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .build()

    private val apiResponseRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(provideOkHttpClient(listOf(AuthorizationInterceptor(), ErrorInterceptor)))
        .addConverterFactory(json.asConverterFactory(contentType.toMediaType()))
        .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(provideOkHttpClient(listOf(AuthorizationInterceptor(), ErrorInterceptor)))
        .addConverterFactory(json.asConverterFactory(contentType.toMediaType()))
        .build()

    private fun provideOkHttpClient(interceptors: List<Interceptor>): OkHttpClient = OkHttpClient.Builder().run {
        interceptors.forEach {
            addInterceptor(it)
        }
        build()
    }

    class AuthorizationInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response = with(chain) {
            @Suppress("UnusedPrivateMember")
            val userId = App.prefs.getUserId()
            val jwtToken =
                App.prefs.getString(App.prefs.jwtTokenKey)
            val newRequest = request().newBuilder()
                .addHeader("authorization", "Bearer $jwtToken")
                .build()
            proceed(newRequest)
        }
    }

    @Suppress("TooGenericExceptionCaught", "RethrowCaughtException", "SwallowedException")
    object ErrorInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response = with(chain) {
            val request = chain.request()
            val response: Response
            try {
                response = chain.proceed(request)
            } catch (e: UnknownHostException) {
                throw IOException("네트워크 에러입니다.")
            }

            if (response.isSuccessful) {
                return response
            }

            val body = response.body?.string() ?: run {
                Timber.e("body is null")
                return response
            }
            Timber.e("body = $body")
            try {
                val error = json.decodeFromString<ErrorEnvelope>(body)
                throw ApiException(error)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    class ApiException(val error: ErrorEnvelope) : IOException(error.message)

    val signUpService: SignUpService = signUpRetrofit.create(SignUpService::class.java)
    val userService: UserService = retrofit.create(UserService::class.java)
    val recommendService: RecommendService = retrofit.create(RecommendService::class.java)
    val contentDetailService: ContentDetailService = retrofit.create(ContentDetailService::class.java)
    val searchService: SearchService = retrofit.create(SearchService::class.java)
    val bookmarkService: BookmarkService = retrofit.create(BookmarkService::class.java)
    val likeToggleService: LikeToggleService = retrofit.create(LikeToggleService::class.java)
    val reportService: ReportService = apiResponseRetrofit.create(ReportService::class.java)
    val myContentService: MyContentService = retrofit.create(MyContentService::class.java)
    val onBoardingService: OnBoardingService = retrofit.create(OnBoardingService::class.java)
    val editProfileService: EditProfileService = apiResponseRetrofit.create(EditProfileService::class.java)
}
