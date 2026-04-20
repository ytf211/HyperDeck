package com.hyperdeck.ui.tools.settings

import android.content.Context
import com.hyperdeck.data.model.SettingsCategory
import com.hyperdeck.data.model.SettingsItem

object SettingsTextResolver {

    fun categoryTitle(context: Context, category: SettingsCategory): String {
        return resolveString(context, category.categoryKey, category.category)
    }

    fun categoryTitle(context: Context, rawCategory: String): String {
        return resolveString(context, fallbackCategoryKey(rawCategory), rawCategory)
    }

    fun itemTitle(context: Context, item: SettingsItem): String {
        return resolveString(context, item.titleKey, item.title)
    }

    fun itemDescription(context: Context, item: SettingsItem): String {
        if (item.description.isBlank() && item.descriptionKey.isBlank()) {
            return ""
        }
        return resolveString(context, item.descriptionKey, item.description)
    }

    private fun resolveString(context: Context, explicitKey: String, fallback: String): String {
        val key = explicitKey.ifBlank { fallbackStringKey(fallback) }
        if (key.isBlank()) return fallback
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        return if (resId != 0) context.getString(resId) else fallback
    }

    private fun fallbackStringKey(rawValue: String): String {
        return fallbackCategoryKey(rawValue).ifBlank {
            when (rawValue) {
                "窗口动画缩放" -> "settings_item_window_animation_scale_title"
                "控制窗口动画时长倍数" -> "settings_item_window_animation_scale_description"
                "过渡动画缩放" -> "settings_item_transition_animation_scale_title"
                "控制过渡动画时长倍数" -> "settings_item_transition_animation_scale_description"
                "动画程序时长缩放" -> "settings_item_animator_duration_scale_title"
                "控制动画程序时长倍数" -> "settings_item_animator_duration_scale_description"
                "屏幕亮度" -> "settings_item_screen_brightness_title"
                "手动设置屏幕亮度 (0-255)" -> "settings_item_screen_brightness_description"
                "屏幕超时" -> "settings_item_screen_timeout_title"
                "自动息屏时间 (毫秒)" -> "settings_item_screen_timeout_description"
                "USB 调试" -> "settings_item_usb_debug_title"
                "启用/禁用 USB 调试" -> "settings_item_usb_debug_description"
                "保持唤醒" -> "settings_item_stay_awake_title"
                "充电时保持屏幕常亮" -> "settings_item_stay_awake_description"
                "应用预加载开关" -> "settings_item_app_preload_title"
                "控制 touch_prestart_opt_config 的应用预加载策略" ->
                    "settings_item_app_preload_description"
                else -> ""
            }
        }
    }

    private fun fallbackCategoryKey(rawCategory: String): String {
        return when (rawCategory) {
            "动画相关" -> "settings_category_animation"
            "显示" -> "settings_category_display"
            "开发者选项" -> "settings_category_developer"
            else -> ""
        }
    }
}
