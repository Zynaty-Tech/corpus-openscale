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
package com.health.openscale.qa

import com.health.openscale.ui.navigation.Routes
import com.health.openscale.ui.qa.screenNameOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The mqa screen map (debug asset) lists exactly the destinations the code tags, and every
 * overlay entry (`<Host>.<Kind>`, or `<Kind>` for an app-wide overlay) names a host screen and
 * an overlay kind that the code tags.
 */
class ScreenMapTest {

    private val uiDir = File("src/main/java/com/health/openscale/ui")
    private val map = File("src/debug/assets/mqa-screens.json")

    @Test
    fun everyRouteHasAScreenName() {
        Routes::class.java.declaredFields
            .filter { it.type == String::class.java && java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.get(null) as String }
            .forEach { route -> assertNotNull("no screen tag name for route $route", screenNameOf(route)) }
    }

    @Test
    fun screenMapMatchesTheTagsInCode() {
        val names = File(uiDir, "qa/ScreenTags.kt").readText()
            .substringAfter("fun screenNameOf")
            .let { Regex("""->\s*"(\w+)"""").findAll(it).map { m -> m.groupValues[1] }.toSet() }
        val literal = uiDir.walk().filter { it.extension == "kt" }
            .flatMap { Regex("""ScreenTagged\("(\w+)"\)""").findAll(it.readText()).map { m -> m.groupValues[1] } }
            .toSet()
        val sources = uiDir.walk().filter { it.extension == "kt" }.map { it.readText() }.toList()
        val overlayKinds = sources.flatMap { text ->
            Regex("""overlayRoot\((?:overlayTag \?: )?"(\w+)"\)|overlayTag = "(\w+)"""").findAll(text)
                .map { m -> m.groupValues[1].ifEmpty { m.groupValues[2] } }
        }.toSet() + "Drawer"
        val inMap = Regex("""\"tag\":\s*\"([\w.]+)\"""").findAll(map.readText()).map { it.groupValues[1] }.toList()
        assertEquals("a tag is listed twice", inMap.size, inMap.toSet().size)
        val destinations = inMap.filter { '.' !in it && it !in overlayKinds }
        assertEquals((names + literal).toSortedSet(), destinations.toSortedSet())
        inMap.filter { '.' in it }.forEach { tag ->
            val (host, kind) = tag.split('.', limit = 2)
            assertTrue("$tag: no screen $host", host in names + literal)
            assertTrue("$tag: no overlay kind $kind in code", kind in overlayKinds)
        }
    }
}
