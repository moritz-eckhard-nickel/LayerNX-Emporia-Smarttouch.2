package com.layernx.launcher

import android.app.Application
import com.layernx.launcher.data.AppRepository

class LayerNXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppRepository.init(this)
    }
}
