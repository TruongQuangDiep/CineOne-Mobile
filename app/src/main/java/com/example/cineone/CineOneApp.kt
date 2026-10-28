package com.example.cineone

import android.app.Application
import com.example.cineone.di.NetworkModule
import com.example.cineone.di.RepositoryModule
import com.example.cineone.di.UseCaseModule

class CineOneApp : Application() {

    lateinit var networkModule: NetworkModule
        private set

    lateinit var repositoryModule: RepositoryModule
        private set

    lateinit var useCaseModule: UseCaseModule
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        networkModule = NetworkModule(this)
        repositoryModule = RepositoryModule(networkModule)
        useCaseModule = UseCaseModule(repositoryModule)
    }

    companion object {
        lateinit var instance: CineOneApp
            private set
    }
}