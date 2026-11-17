package fr.isep.campuspass

import android.app.Application

class CampusPassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
