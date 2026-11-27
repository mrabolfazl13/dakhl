package ir.dakhlokharj.core

import com.google.common.truth.Truth.assertThat
import ir.dakhlokharj.core.date.JalaliDate
import java.time.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

class JalaliDateTest {

    @Test
    fun nowruz1403MapsToMarch20() {
        val date = JalaliDate(1403, 1, 1)
        assertThat(date.toLocalDate()).isEqualTo(LocalDate.of(2024, 3, 20))
    }

    @Test
    fun roundTripThroughGregorianKeepsDate() {
        val date = JalaliDate(1402, 11, 22)
        assertThat(JalaliDate.fromLocalDate(date.toLocalDate())).isEqualTo(date)
    }

    @Test
    fun firstSixMonthsHaveThirtyOneDays() {
        (1..6).forEach { month ->
            assertThat(JalaliDate(1403, month, 1).monthLength).isEqualTo(31)
        }
    }

    @Test
    fun esfandLengthFollowsLeapYear() {
        assertThat(JalaliDate(1403, 12, 1).monthLength).isEqualTo(30)
        assertThat(JalaliDate(1404, 12, 1).monthLength).isEqualTo(29)
    }

    @Test
    fun plusDaysCrossesMonthBoundary() {
        val date = JalaliDate(1403, 1, 31)
        assertThat(date.plusDays(1)).isEqualTo(JalaliDate(1403, 2, 1))
    }

    @Test
    fun plusMonthsClampsDayToMonthLength() {
        val date = JalaliDate(1403, 6, 31)
        assertThat(date.plusMonths(1)).isEqualTo(JalaliDate(1403, 7, 30))
    }

    @Test
    fun weekdayIndexStartsOnSaturday() {
        assertThat(JalaliDate.weekdayIndex(java.time.DayOfWeek.SATURDAY)).isEqualTo(0)
        assertThat(JalaliDate.weekdayIndex(java.time.DayOfWeek.FRIDAY)).isEqualTo(6)
    }

    @Test
    fun startAndEndOfMonthAreCorrect() {
        val date = JalaliDate(1403, 2, 15)
        assertThat(date.startOfMonth).isEqualTo(JalaliDate(1403, 2, 1))
        assertThat(date.endOfMonth).isEqualTo(JalaliDate(1403, 2, 31))
    }

    @Test
    fun startAndEndOfYearAreCorrect() {
        val date = JalaliDate(1403, 5, 10)
        assertThat(date.startOfYear).isEqualTo(JalaliDate(1403, 1, 1))
        assertThat(date.endOfYear).isEqualTo(JalaliDate(1403, 12, 30))
    }

    @Test
    fun comparisonFollowsCalendarOrder() {
        assertThat(JalaliDate(1403, 1, 1) < JalaliDate(1403, 1, 2)).isTrue()
        assertThat(JalaliDate(1403, 2, 1).isAfter(JalaliDate(1403, 1, 31))).isTrue()
    }

    @Test
    fun invalidDayIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { JalaliDate(1403, 7, 31) }
    }

    @Test
    fun invalidMonthIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { JalaliDate(1403, 13, 1) }
    }

    @Test
    fun shortFormatUsesLatinDigitsAndPadding() {
        assertThat(JalaliDate(1403, 1, 5).formatShort()).isEqualTo("1403/01/05")
    }

    @Test
    fun monthNameMatchesCalendar() {
        assertThat(JalaliDate(1403, 12, 1).monthName).isEqualTo("اسفند")
    }

    @Test
    fun epochDayRoundTripIsStable() {
        val date = JalaliDate(1404, 7, 3)
        assertThat(JalaliDate.fromEpochDay(date.toEpochDay())).isEqualTo(date)
    }
}
