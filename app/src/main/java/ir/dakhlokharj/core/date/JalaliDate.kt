package ir.dakhlokharj.core.date

import ir.dakhlokharj.core.format.PersianText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Solar Hijri (Jalali) calendar utilities: a pure Kotlin port of the well known
 * jalaali-js algorithm, verified by unit tests against known Gregorian dates.
 */
object JalaliCalendar {

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    data class CalendarInfo(val leap: Int, val gregorianYear: Int, val march: Int)

    private fun div(a: Int, b: Int): Int = a / b
    private fun mod(a: Int, b: Int): Int = a % b

    fun info(jy: Int, withoutLeap: Boolean = false): CalendarInfo {
        val bl = BREAKS.size
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        if (jy < jp || jy >= BREAKS[bl - 1]) {
            throw IllegalArgumentException("Invalid Jalali year: $jy")
        }
        var i = 1
        while (i < bl) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
            i++
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        var leap = 0
        if (!withoutLeap) {
            if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
            leap = mod(mod(n + 1, 33) - 1, 4)
            if (leap == -1) leap = 4
        }
        return CalendarInfo(leap, gy, march)
    }

    fun isLeapYear(jy: Int): Boolean = info(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        isLeapYear(jy) -> 30
        else -> 29
    }

    private fun gregorianToJdn(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
            div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    private fun jdnToGregorian(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j = j + div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return Triple(gy, gm, gd)
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val calendar = info(jy, true)
        val jdn = gregorianToJdn(calendar.gregorianYear, 3, calendar.march) +
            (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
        return jdnToGregorian(jdn)
    }

    fun toJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val jdn = gregorianToJdn(gy, gm, gd)
        val gregorian = jdnToGregorian(jdn)
        var jy = gregorian.first - 621
        val calendar = info(jy)
        val jdn1f = gregorianToJdn(gregorian.first, 3, calendar.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                return Triple(jy, 1 + div(k, 31), mod(k, 31) + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (calendar.leap == 1) k += 1
        }
        return Triple(jy, 7 + div(k, 30), mod(k, 30) + 1)
    }
}

data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {

    init {
        require(month in 1..12) { "month out of range: $month" }
        require(day in 1..JalaliCalendar.monthLength(year, month)) { "day out of range: $day" }
    }

    val monthName: String get() = MONTH_NAMES[month - 1]
    val weekdayName: String get() = weekdayName(toLocalDate().dayOfWeek)
    val weekdayIndex: Int get() = weekdayIndex(toLocalDate().dayOfWeek)

    fun toLocalDate(): LocalDate {
        val (gy, gm, gd) = JalaliCalendar.toGregorian(year, month, day)
        return LocalDate.of(gy, gm, gd)
    }

    fun toEpochDay(): Long = toLocalDate().toEpochDay()

    fun plusDays(days: Long): JalaliDate = fromEpochDay(toEpochDay() + days)

    fun plusMonths(months: Int): JalaliDate {
        val total = (year * 12 + (month - 1)) + months
        val newYear = total / 12
        val newMonth = total % 12 + 1
        val maxDay = JalaliCalendar.monthLength(newYear, newMonth)
        return JalaliDate(newYear, newMonth, day.coerceAtMost(maxDay))
    }

    val isLeapYear: Boolean get() = JalaliCalendar.isLeapYear(year)
    val monthLength: Int get() = JalaliCalendar.monthLength(year, month)
    val startOfMonth: JalaliDate get() = JalaliDate(year, month, 1)
    val endOfMonth: JalaliDate get() = JalaliDate(year, month, monthLength)
    val startOfYear: JalaliDate get() = JalaliDate(year, 1, 1)
    val endOfYear: JalaliDate get() = JalaliDate(year, 12, JalaliCalendar.monthLength(year, 12))

    fun isBefore(other: JalaliDate): Boolean = this < other

    fun isAfter(other: JalaliDate): Boolean = this > other

    fun formatShort(): String = "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"

    fun formatLong(): String = "$day $monthName $year"

    fun formatFull(): String = "$weekdayName، ${formatLong()}"

    fun formatPersianDigits(): String = PersianText.toPersianDigits(formatLong())

    override fun compareTo(other: JalaliDate): Int = toEpochDay().compareTo(other.toEpochDay())

    override fun toString(): String = formatShort()

    companion object {
        val MONTH_NAMES = arrayOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )
        val WEEKDAY_NAMES = arrayOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

        fun fromEpochDay(epochDay: Long): JalaliDate {
            val date = LocalDate.ofEpochDay(epochDay)
            val (jy, jm, jd) = JalaliCalendar.toJalali(date.year, date.monthValue, date.dayOfMonth)
            return JalaliDate(jy, jm, jd)
        }

        fun fromLocalDate(date: LocalDate): JalaliDate = fromEpochDay(date.toEpochDay())

        fun today(zoneId: ZoneId = ZoneId.systemDefault()): JalaliDate =
            fromLocalDate(LocalDate.now(zoneId))

        fun weekdayName(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
            DayOfWeek.SATURDAY -> WEEKDAY_NAMES[0]
            DayOfWeek.SUNDAY -> WEEKDAY_NAMES[1]
            DayOfWeek.MONDAY -> WEEKDAY_NAMES[2]
            DayOfWeek.TUESDAY -> WEEKDAY_NAMES[3]
            DayOfWeek.WEDNESDAY -> WEEKDAY_NAMES[4]
            DayOfWeek.THURSDAY -> WEEKDAY_NAMES[5]
            DayOfWeek.FRIDAY -> WEEKDAY_NAMES[6]
        }

        fun weekdayIndex(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
            DayOfWeek.SATURDAY -> 0
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 5
            DayOfWeek.FRIDAY -> 6
        }
    }
}