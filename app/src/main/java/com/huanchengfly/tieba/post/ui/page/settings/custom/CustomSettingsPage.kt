package com.huanchengfly.tieba.post.ui.page.settings.custom

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.glass.IosColorPicker
import com.huanchengfly.tieba.post.ui.glass.IosColorPickerLabels
import com.huanchengfly.tieba.post.ui.glass.IosGroupedList
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.page.destinations.FontSizePageDestination
import com.huanchengfly.tieba.post.ui.page.settings.IosSettingsScaffold
import com.huanchengfly.tieba.post.ui.page.settings.SettingsGroup
import com.huanchengfly.tieba.post.ui.page.settings.components.ChoiceSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SwitchSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.fontsize.rememberFontScaleLabel
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.formatColorHex
import com.huanchengfly.tieba.post.utils.parseColorHexOrNull
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private const val DefaultSeedColorHex = "#FF2196F3"

private fun parseSeedColor(hex: String): Color =
    Color(parseColorHexOrNull(hex) ?: 0xFF2196F3.toInt())

@Destination
@Composable
fun CustomSettingsPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    var followSystemNight by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey("follow_system_night"),
        defaultValue = true
    )
    var darkTheme by rememberPreferenceAsMutableState(
        key = stringPreferencesKey(ThemeUtil.KEY_DARK_THEME),
        defaultValue = ThemeUtil.THEME_AMOLED_DARK
    )
    var schemeVariant by rememberPreferenceAsMutableState(
        key = stringPreferencesKey("theme_scheme_variant"),
        defaultValue = "TonalSpot"
    )
    var useSeedTheme by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey("use_seed_theme"),
        defaultValue = true
    )
    var seedColorPref by rememberPreferenceAsMutableState(
        key = stringPreferencesKey("custom_primary_color"),
        defaultValue = DefaultSeedColorHex
    )
    // 与「降低透明度」开关共用同一份状态：单一状态源，避免两处读数不一致
    var reduceTransparency by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey("reduceTransparency"),
        defaultValue = false
    )
    val useDynamicColorTheme by context.dataStore.collectPreferenceAsState(
        key = booleanPreferencesKey("useDynamicColorTheme"),
        defaultValue = false
    )
    val currentIsNight = ThemeUtil.isNightMode()
    var showColorPicker by remember { mutableStateOf(false) }

    val themeMode = when {
        followSystemNight -> "auto"
        currentIsNight -> "dark"
        else -> "light"
    }
    val themeModeIcon = when (themeMode) {
        "auto" -> "circle.lefthalf.filled"
        "light" -> "sun.max.fill"
        else -> "moon.fill"
    }
    val darkStyleNames = mapOf(
        ThemeUtil.THEME_GREY_DARK to stringResource(id = R.string.title_dark_style_grey),
        ThemeUtil.THEME_AMOLED_DARK to stringResource(id = R.string.title_dark_style_amoled),
    )
    val darkStyleOptions = listOf(
        ThemeUtil.THEME_GREY_DARK,
        ThemeUtil.THEME_AMOLED_DARK,
    ).map { it to (darkStyleNames[it] ?: it) }

    val schemeVariants = listOf(
        "TonalSpot", "Neutral", "Vibrant", "Expressive", "Fidelity",
        "Content", "Monochrome", "Rainbow", "FruitSalad",
    )
    val variantLabels = mapOf(
        "TonalSpot" to stringResource(id = R.string.scheme_tonal_spot),
        "Neutral" to stringResource(id = R.string.scheme_neutral),
        "Vibrant" to stringResource(id = R.string.scheme_vibrant),
        "Expressive" to stringResource(id = R.string.scheme_expressive),
        "Fidelity" to stringResource(id = R.string.scheme_fidelity),
        "Content" to stringResource(id = R.string.scheme_content),
        "Monochrome" to stringResource(id = R.string.scheme_monochrome),
        "Rainbow" to stringResource(id = R.string.scheme_rainbow),
        "FruitSalad" to stringResource(id = R.string.scheme_fruit_salad),
    )
    val accentOptions = listOf(
        "default" to stringResource(id = R.string.title_theme_accent_default),
        "custom" to stringResource(id = R.string.title_theme_accent_custom),
    )
    val accentSelected = if (seedColorPref.equals(DefaultSeedColorHex, ignoreCase = true)) {
        "default"
    } else {
        "custom"
    }

    /** 选定种子色的统一写入（默认色/自定义色共用） */
    fun applySeedColor(hex: String) {
        seedColorPref = hex
        useSeedTheme = true
        ThemeUtil.setUseDynamicTheme(false)
        ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
    }

    IosSettingsScaffold(
        title = stringResource(id = R.string.title_settings_custom),
        onNavigateBack = { navigator.navigateUp() },
        overlay = {
            IosColorPicker(
                visible = showColorPicker,
                selectedColor = parseSeedColor(seedColorPref),
                onColorSelected = { color ->
                    applySeedColor(formatColorHex(color.toArgb()))
                },
                onDismiss = { showColorPicker = false },
                title = stringResource(id = R.string.title_theme_color_picker),
                labels = IosColorPickerLabels(
                    grid = stringResource(id = R.string.color_picker_grid),
                    spectrum = stringResource(id = R.string.color_picker_spectrum),
                    sliders = stringResource(id = R.string.color_picker_sliders),
                    opacity = stringResource(id = R.string.color_picker_opacity),
                ),
            )
        },
    ) {
        // ── 主题（对齐 MeiloX「设置 - 通用 - 外观」：一行一选择 + 说明）
        item {
            SettingsGroup(stringResource(id = R.string.title_theme)) {
                ChoiceSettingRow(
                    systemName = themeModeIcon,
                    title = stringResource(id = R.string.title_theme_mode),
                    options = listOf(
                        "auto" to stringResource(id = R.string.title_theme_mode_auto),
                        "light" to stringResource(id = R.string.title_theme_mode_light),
                        "dark" to stringResource(id = R.string.title_theme_mode_dark),
                    ),
                    selected = themeMode,
                    onSelected = { mode ->
                        when (mode) {
                            "auto" -> followSystemNight = true
                            "light" -> {
                                followSystemNight = false
                                if (ThemeUtil.isNightMode()) ThemeUtil.switchNightMode()
                            }
                            "dark" -> {
                                followSystemNight = false
                                if (!ThemeUtil.isNightMode()) ThemeUtil.switchNightMode()
                            }
                        }
                    },
                )
                Text(
                    stringResource(id = R.string.title_theme_mode_hint),
                    style = IosTypography.subheadline,
                    color = LocalGlassColors.current.secondaryContent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                ChoiceSettingRow(
                    systemName = "moon.fill",
                    title = stringResource(id = R.string.title_dark_style),
                    options = darkStyleOptions,
                    selected = darkTheme,
                    onSelected = { id ->
                        darkTheme = id
                        if (ThemeUtil.isNightMode()) {
                            ThemeUtil.switchTheme(id, recordOldTheme = false)
                        }
                    },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchSettingRow(
                        systemName = "circle.lefthalf.filled",
                        title = stringResource(id = R.string.title_follow_system_color),
                        summary = stringResource(id = R.string.summary_follow_system_color),
                        checked = useDynamicColorTheme,
                        onCheckedChange = { enabled ->
                            useSeedTheme = !enabled
                            ThemeUtil.setUseDynamicTheme(enabled)
                            ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
                        },
                    )
                }
                ChoiceSettingRow(
                    systemName = "paintpalette.fill",
                    title = stringResource(id = R.string.title_accent_color),
                    options = accentOptions,
                    selected = accentSelected,
                    enabled = !useDynamicColorTheme,
                    onSelected = { option ->
                        if (option == "custom") {
                            useSeedTheme = true
                            ThemeUtil.setUseDynamicTheme(false)
                            showColorPicker = true
                        } else {
                            applySeedColor(DefaultSeedColorHex)
                        }
                    },
                )
                ChoiceSettingRow(
                    systemName = "circle.grid.2x2.fill",
                    title = stringResource(id = R.string.title_scheme_style),
                    options = schemeVariants.map { it to (variantLabels[it] ?: it) },
                    selected = schemeVariant,
                    onSelected = { variant ->
                        schemeVariant = variant
                        if (useSeedTheme && !useDynamicColorTheme) {
                            ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
                        }
                    },
                )
            }
        }

        // ── 界面（对齐 MeiloX 外观设置的三项）
        item {
            SettingsGroup(stringResource(id = R.string.title_interface)) {
                SwitchSettingRow(
                    systemName = "rectangle.portrait.and.arrow.forward",
                    title = stringResource(id = R.string.title_fixed_bottom_bar),
                    summary = stringResource(id = R.string.summary_fixed_bottom_bar),
                    key = "keepBottomBarExpanded",
                    defaultValue = false,
                )
                SwitchSettingRow(
                    systemName = "circle.grid.2x2.fill",
                    title = stringResource(id = R.string.title_liquid_glass),
                    summary = stringResource(id = R.string.summary_liquid_glass),
                    key = "liquidGlassEnabled",
                    defaultValue = true,
                    enabled = !reduceTransparency,
                )
                SwitchSettingRow(
                    systemName = "circle.lefthalf.filled",
                    title = stringResource(id = R.string.title_reduce_transparency),
                    summary = stringResource(id = R.string.summary_reduce_transparency),
                    checked = reduceTransparency,
                    onCheckedChange = { reduceTransparency = it },
                )
            }
        }

        // ── 更多
        item {
            SettingsGroup(stringResource(id = R.string.title_theme_more)) {
                SwitchSettingRow(
                    systemName = "photo",
                    title = stringResource(id = R.string.title_settings_status_bar_darker),
                    summary = stringResource(id = R.string.summary_settings_status_bar_darker),
                    key = "status_bar_darker",
                    defaultValue = true,
                )
                SwitchSettingRow(
                    systemName = "paintpalette",
                    title = stringResource(id = R.string.tip_toolbar_primary_color),
                    summary = stringResource(id = R.string.tip_toolbar_primary_color_summary),
                    key = "toolbarPrimaryColor",
                    defaultValue = false,
                )
                SwitchSettingRow(
                    systemName = "list.bullet",
                    title = stringResource(id = R.string.settings_forum_single),
                    key = "listSingle",
                    defaultValue = false,
                )
                SwitchSettingRow(
                    systemName = "safari",
                    title = stringResource(id = R.string.title_hide_explore),
                    key = "hideExplore",
                    defaultValue = false,
                )
                SwitchSettingRow(
                    systemName = "rectangle.grid.1x2",
                    title = stringResource(id = R.string.title_floating_bottom_nav),
                    summary = stringResource(id = R.string.summary_floating_bottom_nav),
                    key = "floatingBottomNav",
                    defaultValue = true,
                )
                SwitchSettingRow(
                    systemName = "arrow.up.circle.fill",
                    title = stringResource(id = R.string.title_lift_up_bottom_bar),
                    summary = stringResource(id = R.string.summary_lift_up_bottom_bar),
                    key = "liftUpBottomBar",
                    defaultValue = true,
                )
            }
        }

        // ── 字体
        item {
            IosGroupedList {
                SettingRow(
                    systemName = "textformat.size",
                    title = stringResource(id = R.string.title_custom_font_size),
                    summary = rememberFontScaleLabel(context.appPreferences.fontScale),
                    onClick = { navigator.navigate(FontSizePageDestination) }
                )
            }
        }
    }
}
