package com.example.allowcamera

import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

object CameraHooks {
    private const val PREFS_NAME = "camera_prefs"
    private const val KEY_PACKAGE = "selected_camera_package"
    private const val MODULE_PKG = "com.example.allowcamera"

    private fun getTargetPackage(): String? {
        return try {
            val prefs = XSharedPreferences(MODULE_PKG, PREFS_NAME)
            prefs.makeWorldReadable()
            prefs.reload()
            val pkg = prefs.getString(KEY_PACKAGE, null)
            if (pkg.isNullOrBlank()) null else pkg
        } catch (_: Throwable) {
            null
        }
    }

    fun hook(lpparam: LoadPackageParam) {
        val target = getTargetPackage() ?: return
        val cl = lpparam.classLoader

        // Android 10-13: SystemUI KeyguardBottomAreaView
        hookResolveCameraIntent(cl, "com.android.systemui.statusbar.KeyguardBottomAreaView", target)
        hookResolveCameraIntent(cl, "com.android.systemui.statusbar.phone.KeyguardBottomAreaView", target)

        // Android 12+: KeyguardCameraMediator
        hookResolveCameraIntent(cl, "com.android.systemui.keyguard.KeyguardCameraMediator", target)
        hookResolveCameraIntent(cl, "com.android.systemui.keyguard.KeyguardBottomAreaView", target)
    }

    private fun hookResolveCameraIntent(classLoader: ClassLoader, className: String, targetPackage: String) {
        try {
            val clazz = findClass(className, classLoader)
            findAndHookMethod(clazz, "resolveCameraIntent", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val intent = param.result as? Intent ?: return
                    intent.setPackage(targetPackage)
                }
            })
        } catch (_: Throwable) { }
    }
}
