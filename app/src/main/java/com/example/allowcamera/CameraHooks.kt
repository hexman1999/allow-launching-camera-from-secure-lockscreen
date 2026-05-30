package com.example.allowcamera

import android.content.Intent
import android.provider.MediaStore
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

    fun hook(module: XposedModule, param: PackageReadyParam) {
        val target = getTargetPackage(module)
        val cl = param.classLoader

        hookResolveCameraIntent(module, cl, "com.android.systemui.statusbar.KeyguardBottomAreaView", target)
        hookResolveCameraIntent(module, cl, "com.android.systemui.statusbar.phone.KeyguardBottomAreaView", target)
        hookResolveCameraIntent(module, cl, "com.android.systemui.keyguard.KeyguardCameraMediator", target)
        hookResolveCameraIntent(module, cl, "com.android.systemui.keyguard.KeyguardBottomAreaView", target)
    }

    private fun hookResolveCameraIntent(module: XposedModule, classLoader: ClassLoader, className: String, targetPackage: String?) {
        try {
            val clazz = Class.forName(className, false, classLoader)
            val method = clazz.getDeclaredMethod("resolveCameraIntent")
            method.isAccessible = true
            module.hook(method).intercept { chain ->
                val intent = chain.proceed() as? Intent
                if (intent != null) {
                    if (targetPackage != null) {
                        intent.setPackage(targetPackage)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                intent
            }
        } catch (_: Throwable) { }
    }
}
