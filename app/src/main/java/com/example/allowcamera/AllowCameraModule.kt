package com.example.allowcamera

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class AllowCameraModule : XposedModule() {
    override fun onModuleLoaded(param: ModuleLoadedParam) {
        Log.d(TAG, "module loaded, process=${param.processName}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        when (param.packageName) {
            "com.android.systemui" -> {
                Log.d(TAG, "SystemUI ready — applying camera shortcut hook")
                CameraHooks.hookSystemUi(this, param)
            }
            else -> {
                Log.d(TAG, "camera app ready (${param.packageName}) — applying window-flag hook")
                CameraHooks.hookCameraApp(this, param)
            }
        }
    }

    companion object {
        const val TAG = "AllowCamera"
    }
}
