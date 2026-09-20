package com.sycompany.duomorph

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController

class MainActivity : Activity() {

    private lateinit var profile: DeviceProfile
    private lateinit var homeView: DuoLauncherView
    private lateinit var hinge: HingeController
    private var lastInner: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile = DeviceProfile.current()

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.apply {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val apps = loadApps()
        homeView = DuoLauncherView(this, apps, profile)
        setContentView(homeView)

        hinge = HingeController(this, profile.transitionMs) { p, state ->
            runOnUiThread { homeView.setFoldProgress(p, state) }
        }
        hinge.start()

        homeView.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
            val inner = DeviceProfile.isInner(r-l, b-t)
            if (lastInner == null) {
                lastInner = inner
                hinge.snapToInner(inner)
            } else if (lastInner != inner) {
                lastInner = inner
                hinge.snapToInner(inner)
            }
        }

        maybeRequestHomeRole()
    }

    override fun onResume() {
        super.onResume()
        homeView.post {
            val inner = DeviceProfile.isInner(homeView.width, homeView.height)
            hinge.snapToInner(inner)
        }
    }

    override fun onDestroy() {
        hinge.stop()
        super.onDestroy()
    }

    private fun maybeRequestHomeRole() {
        val rm = getSystemService(RoleManager::class.java) ?: return
        if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            try {
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 42)
            } catch (_: Throwable) {}
        }
    }

    private fun loadApps(): List<AppEntry> {
        val pm = packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val results: List<ResolveInfo> = pm.queryIntentActivities(q, 0)
        return results
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName + "/" + it.activityInfo.name }
            .map {
                AppEntry(
                    label = it.loadLabel(pm)?.toString() ?: it.activityInfo.packageName,
                    component = android.content.ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                    icon = it.loadIcon(pm)
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
