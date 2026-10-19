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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import com.health.openscale.MainActivity
import kotlinx.coroutines.launch

/**
 * Debug-only: `am start -W -n <package>/com.health.openscale.qa.QaSeedActivity [--es scenario <name>]`.
 * Loads the scenario (default `users-measurements`), then opens MainActivity and finishes.
 * For tools that can start an activity but cannot send a broadcast.
 */
class QaSeedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = intent.getStringExtra("scenario") ?: "users-measurements"
        lifecycleScope.launch {
            val result = QaSeeder.seed(applicationContext, scenario)
            Log.i("QaSeed", result.getOrElse { "seed failed: ${it.message}" })
            setResult(if (result.isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED)
            startActivity(Intent(this@QaSeedActivity, MainActivity::class.java))
            finish()
        }
    }
}
