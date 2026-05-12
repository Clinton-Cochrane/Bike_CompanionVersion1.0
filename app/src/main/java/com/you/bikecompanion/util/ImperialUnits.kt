package com.you.bikecompanion.util

import kotlin.math.abs

/**
 * Converts metric values stored in the domain layer (km, km/h, meters) to US customary units
 * for display and for user input that is entered in miles.
 *
 * Persistence and calculations remain metric; conversions happen at the UI boundary.
 */
object ImperialUnits {

    private const val KM_PER_MILE = 1.609344
    private const val METERS_PER_FOOT = 0.3048

    fun kmToMiles(km: Double): Double = km / KM_PER_MILE

    fun milesToKm(miles: Double): Double = miles * KM_PER_MILE

    fun kmhToMph(kmh: Double): Double = kmh / KM_PER_MILE

    fun metersToFeet(m: Double): Double = m / METERS_PER_FOOT

    /**
     * Miles suitable for an editable text field, trimming trailing zeros from fractional values.
     */
    fun milesFromKmToInputString(km: Double): String {
        val mi = kmToMiles(km)
        val roundedWhole = mi.toLong()
        return if (abs(mi - roundedWhole.toDouble()) < 1e-6) {
            roundedWhole.toString()
        } else {
            String.format("%.2f", mi).trimEnd('0').trimEnd('.')
        }
    }
}
