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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only: `am broadcast -a com.health.openscale.QA_SEED -p <package> --es scenario <name>`.
 * Replies `seeded <name>` (RESULT_OK) once the data is committed, or the error (RESULT_CANCELED).
 */
class QaSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scenario = intent.getStringExtra("scenario")
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val result = QaSeeder.seed(context, scenario)
            pending.resultCode = if (result.isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED
            pending.resultData = result.getOrElse { "seed failed: ${it.message}" }.take(200)
            pending.finish()
        }
    }
}
