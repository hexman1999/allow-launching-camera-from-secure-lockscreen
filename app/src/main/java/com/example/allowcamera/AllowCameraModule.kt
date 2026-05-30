package com.example.allowcamera

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class AllowCameraModule : XposedModule() {
    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.android.systemui") return
        Log.d(TAG, "SystemUI loaded, waiting for package ready")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != "com.android.systemui") return
        Log.d(TAG, "SystemUI ready, applying hooks")
        CameraHooks.hook(this, param)
    }

    companion object {
        const val TAG = "AllowCamera"
    }
}
