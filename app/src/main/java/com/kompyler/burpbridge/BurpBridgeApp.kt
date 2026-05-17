package com.kompyler.burpbridge

import android.app.Application
import com.kompyler.burpbridge.ui.BurpBridgeViewModel

class BurpBridgeApp : Application() {

    lateinit var viewModel: BurpBridgeViewModel
        private set

    override fun onCreate() {
        super.onCreate()
        viewModel = BurpBridgeViewModel(this)
    }
}