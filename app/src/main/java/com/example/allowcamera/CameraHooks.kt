package com.example.allowcamera

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

object CameraHooks {
    private const val PREFS_NAME = "camera_prefs"
    private const val KEY_PACKAGE = "selected_camera_package"

    private fun getTargetPackage(module: XposedModule): String? {
        return try {
            val prefs = module.getRemotePreferences(PREFS_NAME)
            val pkg = prefs.getString(KEY_PACKAGE, null)
            if (pkg.isNullOrBlank()) null else pkg
        } catch (_: Throwable) {
            null
        }
    }

    fun hookSystemUi(module: XposedModule, param: PackageReadyParam) {
        val target = getTargetPackage(module)
        val cl = param.classLoader

        val classes = listOf(
            "com.android.systemui.statusbar.KeyguardBottomAreaView",
            "com.android.systemui.statusbar.phone.KeyguardBottomAreaView",
            "com.android.systemui.keyguard.KeyguardCameraMediator",
            "com.android.systemui.keyguard.KeyguardBottomAreaView"
        )

        for (className in classes) {
            try {
                val clazz = Class.forName(className, false, cl)
                val method = clazz.getDeclaredMethod("resolveCameraIntent")
                method.isAccessible = true
                module.hook(method).intercept { chain ->
                    val intent = chain.proceed() as? Intent
                    if (intent != null) {
                        if (target != null) {
                            intent.setPackage(target)
                        }
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                        )
                    }
                    intent
                }
            } catch (_: Throwable) { }
        }
    }

    fun hookCameraApp(module: XposedModule, param: PackageReadyParam) {
        val cl = param.classLoader

        try {
            val activityClass = Class.forName("android.app.Activity", false, cl)
            val onCreateMethod = activityClass.getDeclaredMethod(
                "onCreate", android.os.Bundle::class.java
            )
            module.hook(onCreateMethod).intercept { chain ->
                val activity = chain.getThisObject() as? Activity
                if (activity != null) {
                    val intent = activity.intent
                    val action = intent?.action
                    if (action != null && isCameraAction(action)) {
                        activity.window?.addFlags(
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            activity.setTurnScreenOn(true)
                        }
                    }
                }
                chain.proceed()
            }
        } catch (_: Throwable) { }
    }

    private fun isCameraAction(action: String): Boolean {
        return action == "android.media.action.STILL_IMAGE_CAMERA" ||
               action == "android.media.action.STILL_IMAGE_CAMERA_SECURE" ||
               action == "android.media.action.IMAGE_CAPTURE" ||
               action == "android.media.action.IMAGE_CAPTURE_SECURE" ||
               action == "android.media.action.VIDEO_CAMERA" ||
               action == "android.media.action.VIDEO_CAPTURE"
    }
}
