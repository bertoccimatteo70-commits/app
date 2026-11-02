package com.lifeos.data

/** One unit of a measure family, with conversions to and from the base unit. */
data class MeasureUnit(
    val label: String,
    val family: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

val measureUnits: List<MeasureUnit> = listOf(
    // length, base metre
    MeasureUnit("mm", "Lunghezza", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("cm", "Lunghezza", { it / 100.0 }, { it * 100.0 }),
    MeasureUnit("m", "Lunghezza", { it }, { it }),
    MeasureUnit("km", "Lunghezza", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("pollice", "Lunghezza", { it * 0.0254 }, { it / 0.0254 }),
    MeasureUnit("piede", "Lunghezza", { it * 0.3048 }, { it / 0.3048 }),
    MeasureUnit("miglio", "Lunghezza", { it * 1609.344 }, { it / 1609.344 }),
    // mass, base gram
    MeasureUnit("mg", "Peso", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("g", "Peso", { it }, { it }),
    MeasureUnit("kg", "Peso", { it * 1000.0 }, { it / 1000.0 }),
    MeasureUnit("quintale", "Peso", { it * 100_000.0 }, { it / 100_000.0 }),
    MeasureUnit("libbra", "Peso", { it * 453.59237 }, { it / 453.59237 }),
    MeasureUnit("oncia", "Peso", { it * 28.349523125 }, { it / 28.349523125 }),
    // volume, base litre
    MeasureUnit("ml", "Volume", { it / 1000.0 }, { it * 1000.0 }),
    MeasureUnit("l", "Volume", { it }, { it }),
    MeasureUnit("gallone US", "Volume", { it * 3.785411784 }, { it / 3.785411784 }),
    // temperature, base celsius (offset handled here)
    MeasureUnit("°C", "Temperatura", { it }, { it }),
    MeasureUnit("°F", "Temperatura", { (it - 32) * 5 / 9 }, { it * 9 / 5 + 32 }),
    MeasureUnit("K", "Temperatura", { it - 273.15 }, { it + 273.15 }),
    // data, base byte
    MeasureUnit("B", "Dati", { it }, { it }),
    MeasureUnit("KB", "Dati", { it * 1024.0 }, { it / 1024.0 }),
    MeasureUnit("MB", "Dati", { it * 1024.0 * 1024 }, { it / (1024.0 * 1024) }),
    MeasureUnit("GB", "Dati", { it * 1024.0 * 1024 * 1024 }, { it / (1024.0 * 1024 * 1024) })
)

val measureFamilies: List<String> = measureUnits.map { it.family }.distinct()

fun convertMeasure(value: Double, from: MeasureUnit, to: MeasureUnit): Double {
    require(from.family == to.family) { "Unità non compatibili" }
    return to.fromBase(from.toBase(value))
}

/** Percentage helpers used by the extra calculators. */
fun percentOf(percent: Double, value: Double): Double = value * percent / 100.0

fun applyPercent(value: Double, percent: Double): Double = value * (1 + percent / 100.0)

fun percentageChange(from: Double, to: Double): Double =
    if (from == 0.0) 0.0 else (to - from) / from * 100.0

/** Splits a total into [people] equal shares, guarding against zero. */
fun splitBill(total: Double, people: Int): Double = if (people <= 0) 0.0 else total / people
