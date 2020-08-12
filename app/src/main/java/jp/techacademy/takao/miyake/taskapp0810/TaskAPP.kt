package jp.techacademy.takao.miyake.taskapp0810

import android.app.Application
import io.realm.Realm

class TaskApp:Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
    }
}