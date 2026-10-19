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

import android.content.Context
import com.health.openscale.core.data.ActivityLevel
import com.health.openscale.core.data.GenderType
import com.health.openscale.core.data.InputFieldType
import com.health.openscale.core.data.Measurement
import com.health.openscale.core.data.MeasurementType
import com.health.openscale.core.data.MeasurementValue
import com.health.openscale.core.data.User
import com.health.openscale.core.data.UserGoals
import com.health.openscale.core.facade.MeasurementFacade
import com.health.openscale.core.facade.UserFacade
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.sin

/**
 * Debug-only QA seeds (mqa skill `qa-seeds`). Every scenario first removes all users (their
 * measurements and goals go with them through the foreign keys), then writes fixed data:
 * fixed names, fixed dates, fixed values. Loading a scenario twice gives the same state.
 */
object QaSeeder {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun userFacade(): UserFacade
        fun measurementFacade(): MeasurementFacade
    }

    val SCENARIOS = listOf("users-measurements", "empty")

    /** 2026-09-30T07:00:00Z: the newest seeded measurement. Fixed, never "now". */
    private const val ANCHOR_MILLIS = 1_790_751_600_000L
    private const val DAY_MILLIS = 86_400_000L

    /** Loads [scenario]; returns a short result for the caller (max 200 characters). */
    suspend fun seed(context: Context, scenario: String?): Result<String> = runCatching {
        require(scenario in SCENARIOS) { "unknown scenario $scenario" }
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val users = deps.userFacade()
        val measurements = deps.measurementFacade()

        val types = awaitMeasurementTypes(measurements)

        users.setSelectedUserId(null).getOrThrow()
        users.observeAllUsers().first().forEach { users.deleteUser(it, reseatSelection = false).getOrThrow() }

        when (scenario) {
            "empty" -> "seeded empty"
            else -> {
                val alexId = users.addUser(
                    User(
                        name = "Alex QA",
                        birthDate = 631_152_000_000L, // 1990-01-01
                        gender = GenderType.MALE,
                        heightCm = 178f,
                        activityLevel = ActivityLevel.MODERATE,
                        useAssistedWeighing = false,
                    )
                ).getOrThrow().toInt()
                val samId = users.addUser(
                    User(
                        name = "Sam QA",
                        birthDate = 788_918_400_000L, // 1995-01-01
                        gender = GenderType.FEMALE,
                        heightCm = 165f,
                        activityLevel = ActivityLevel.MILD,
                        useAssistedWeighing = false,
                    )
                ).getOrThrow().toInt()

                val writable = types.filter { it.isEnabled && !it.isDerived && it.inputType == InputFieldType.FLOAT }
                writeSeries(measurements, alexId, writable, count = 40, baseWeight = 84f, slope = -0.12f)
                writeSeries(measurements, samId, writable, count = 6, baseWeight = 61f, slope = 0.02f)

                types.firstOrNull { it.key == MeasurementType.WEIGHT }?.let { weight ->
                    users.insertUserGoal(
                        UserGoals(
                            userId = alexId,
                            measurementTypeId = weight.id,
                            goalValue = 78f,
                            goalTargetDate = ANCHOR_MILLIS + 90 * DAY_MILLIS,
                            startDate = ANCHOR_MILLIS - 39 * DAY_MILLIS,
                        )
                    )
                }
                users.setSelectedUserId(alexId).getOrThrow()
                "seeded users-measurements"
            }
        }
    }

    /** Default types are written on first start, in the background; wait for them (max 10 s). */
    private suspend fun awaitMeasurementTypes(measurements: MeasurementFacade): List<MeasurementType> {
        repeat(50) {
            val types = measurements.getAllMeasurementTypes().first()
            if (types.isNotEmpty()) return types
            delay(200)
        }
        error("no measurement types after 10 s")
    }

    /** One measurement per day, newest at [ANCHOR_MILLIS]; values are a fixed function of the day. */
    private suspend fun writeSeries(
        measurements: MeasurementFacade,
        userId: Int,
        types: List<MeasurementType>,
        count: Int,
        baseWeight: Float,
        slope: Float,
    ) {
        for (day in count - 1 downTo 0) {
            val age = (count - 1 - day).toFloat() // 0 = oldest
            val wave = sin(age * 0.7f) * 0.4f
            val weight = baseWeight + slope * age + wave
            val fat = 24f + slope * age * 0.5f + wave * 0.5f
            val values = types.mapNotNull { type ->
                val v = when (type.key) {
                    MeasurementType.WEIGHT -> weight
                    MeasurementType.BODY_FAT -> fat
                    MeasurementType.WATER -> 55f - wave
                    MeasurementType.MUSCLE -> 38f + age * 0.03f
                    MeasurementType.BONE -> 3.1f
                    else -> null
                } ?: return@mapNotNull null
                MeasurementValue(measurementId = 0, typeId = type.id, floatValue = v)
            }
            measurements.saveMeasurement(
                Measurement(userId = userId, timestamp = ANCHOR_MILLIS - day * DAY_MILLIS),
                values,
            )
        }
    }
}
