package com.raitha.bharosa.di

import android.content.Context
import androidx.room.Room
import com.raitha.bharosa.BuildConfig
import com.raitha.bharosa.data.local.AppDatabase
import com.raitha.bharosa.data.local.dao.CropHistoryDao
import com.raitha.bharosa.data.local.dao.FarmerDao
import com.raitha.bharosa.data.local.dao.SimulatedSnapshotDao
import com.raitha.bharosa.data.local.dao.SoilDataDao
import com.raitha.bharosa.data.remote.WeatherApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideFarmerDao(db: AppDatabase): FarmerDao = db.farmerDao()
    @Provides fun provideSoilDataDao(db: AppDatabase): SoilDataDao = db.soilDataDao()
    @Provides fun provideCropHistoryDao(db: AppDatabase): CropHistoryDao = db.cropHistoryDao()
    @Provides fun provideSimulatedSnapshotDao(db: AppDatabase): SimulatedSnapshotDao = db.simulatedSnapshotDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.OWM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService =
        retrofit.create(WeatherApiService::class.java)
}
