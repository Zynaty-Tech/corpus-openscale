/*
 * openScale
 * Copyright (C) 2025 olie.xdev <olie.xdeveloper@googlemail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.health.openscale.ui.qa

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.health.openscale.BuildConfig
import com.health.openscale.ui.navigation.Routes

/*
 * Screen tags for external QA tools (mqa, UiAutomator, Appium). Invariants:
 * - A tag name is a code name (letters, digits, `_`), never visible or translated text.
 * - `EXPOSE_TEST_TAGS` is false in defaultConfig and true only in the debug build type, so
 *   release, beta and oss publish no tag to the accessibility tree.
 * - Every route registered in AppNavHost has a name in [screenNameOf]; ScreenMapTest fails
 *   when a route and src/debug/assets/mqa-screens.json disagree.
 */

/** The tag name of the screen that hosts the current composition; null outside a screen. */
val LocalScreenName = compositionLocalOf<String?> { null }

/** Publishes the test tags of this composition root as resource ids (debug only). */
fun Modifier.exposeTestTags(): Modifier =
    if (BuildConfig.EXPOSE_TEST_TAGS) this.semantics { testTagsAsResourceId = true } else this

/** Marks the root of a screen or overlay: `screen:<name>`. */
fun Modifier.screenTag(name: String): Modifier = this.testTag("screen:$name")

/**
 * The modifier for the root of a dialog, sheet or picker. Each overlay is its own window, so it
 * exposes its own tags. Named `<HostScreen>.<kind>`, or `<kind>` alone outside any screen
 * (app-wide dialogs drawn beside the NavHost).
 */
@Composable
fun overlayRoot(kind: String): Modifier {
    val host = LocalScreenName.current
    return Modifier.exposeTestTags().screenTag(if (host != null) "$host.$kind" else kind)
}

/** The id of a control: `<HostScreen>.<control>`, or `<control>` outside any screen. */
@Composable
fun controlTag(control: String): Modifier {
    val host = LocalScreenName.current
    return Modifier.testTag(if (host != null) "$host.$control" else control)
}

/** Wraps a screen's content in its tag and tells overlays which screen hosts them. */
@Composable
fun ScreenTagged(name: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalScreenName provides name) {
        Box(Modifier.screenTag(name)) { content() }
    }
}

/** The tag name of a NavHost route pattern (arguments stripped). Null for an unknown route. */
fun screenNameOf(route: String?): String? = when (route?.substringBefore('?')) {
    Routes.OVERVIEW -> "Overview"
    "overview_drilldown" -> "OverviewDrillDown"
    Routes.GRAPH -> "Graph"
    Routes.TABLE -> "Table"
    "table_drilldown" -> "TableDrillDown"
    "measurement_comparison" -> "MeasurementComparison"
    Routes.STATISTICS -> "Statistics"
    Routes.INSIGHTS -> "Insights"
    Routes.SETTINGS -> "Settings"
    Routes.GENERAL_SETTINGS -> "GeneralSettings"
    Routes.USER_SETTINGS -> "UserSettings"
    Routes.MEASUREMENT_TYPES -> "MeasurementTypeSettings"
    Routes.BLUETOOTH_SETTINGS -> "BluetoothSettings"
    Routes.CHART_SETTINGS -> "ChartSettings"
    Routes.DATA_MANAGEMENT_SETTINGS -> "DataManagementSettings"
    Routes.ABOUT_SETTINGS -> "AboutSettings"
    Routes.USER_DETAIL -> "UserDetail"
    Routes.MEASUREMENT_DETAIL -> "MeasurementDetail"
    Routes.MEASUREMENT_TYPE_DETAIL -> "MeasurementTypeDetail"
    Routes.BLUETOOTH_DETAIL -> "BluetoothDetail"
    else -> null
}
