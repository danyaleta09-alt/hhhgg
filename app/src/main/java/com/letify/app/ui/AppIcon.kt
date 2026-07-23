package com.letify.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.letify.app.R

/**
 * The selectable home-screen icon variants. [key] is what we persist; [alias]
 * is the manifest <activity-alias> suffix we enable/disable to swap the icon.
 * [title] is the thumbnail's content description, and [thumbRes] is
 * the preview image generated alongside the launcher resources. Using a direct
 * R reference (not getIdentifier) keeps the thumbnails safe from resource
 * shrinking.
 */
enum class AppIconVariant(
    val key: String,
    val alias: String,
    val title: String,
    @param:DrawableRes val thumbRes: Int,
) {
    Coral("coral", "MainAliasCoral", "Coral sunset", R.drawable.icon_thumb_coral),
    Peach("peach", "MainAliasPeach", "Peach lavender", R.drawable.icon_thumb_peach),
    Indigo("indigo", "MainAliasIndigo", "Deep indigo", R.drawable.icon_thumb_indigo),
    Seagreen("seagreen", "MainAliasSeagreen", "Sea green", R.drawable.icon_thumb_seagreen),
    Golden("golden", "MainAliasGolden", "Golden hour", R.drawable.icon_thumb_golden);

    companion object {
        val DEFAULT = Coral
        fun fromKey(key: String?): AppIconVariant =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * Apply the chosen launcher icon by enabling its alias and disabling every
 * other one. Uses DONT_KILL_APP so the running app is not torn down — the
 * launcher updates the icon shortly after (some launchers need a moment / a
 * home-screen refresh). No-op if [variant] is already the only enabled alias.
 */
fun applyAppIcon(context: Context, variant: AppIconVariant) {
    val pm = context.packageManager
    val pkg = context.packageName
    AppIconVariant.entries.forEach { v ->
        val component = ComponentName(pkg, "$pkg.${v.alias}")
        val target = if (v == variant) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (pm.getComponentEnabledSetting(component) != target) {
            pm.setComponentEnabledSetting(component, target, PackageManager.DONT_KILL_APP)
        }
    }
}
