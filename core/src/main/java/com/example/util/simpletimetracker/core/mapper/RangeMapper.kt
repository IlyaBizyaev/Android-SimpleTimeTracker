package com.example.util.simpletimetracker.core.mapper

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.viewData.RangeViewData
import com.example.util.simpletimetracker.core.viewData.RangesViewData
import com.example.util.simpletimetracker.core.viewData.SelectDateViewData
import com.example.util.simpletimetracker.core.viewData.SelectRangeViewData
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.model.DayOfWeek
import com.example.util.simpletimetracker.domain.model.Range
import com.example.util.simpletimetracker.domain.model.RangeLength
import com.example.util.simpletimetracker.domain.model.Record
import com.example.util.simpletimetracker.domain.model.RunningRecord
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class RangeMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val timeMapper: TimeMapper,
) {

    fun mapToRanges(currentRange: RangeLength, addSelection: Boolean = true): RangesViewData {
        val selectDateButton = mapToSelectDateName(currentRange)
            ?.takeIf { addSelection }?.let(::listOf) ?: emptyList()
        val selectRangeButton = mapToSelectRange()
            .takeIf { addSelection }?.let(::listOf) ?: emptyList()

        val data = selectDateButton + selectRangeButton + ranges.map(::mapToRangeName)
        val selectedPosition = data.indexOfFirst {
            (it as? RangeViewData)?.range == currentRange
        }.takeUnless { it == -1 }.orZero()

        return RangesViewData(
            items = data,
            selectedPosition = selectedPosition
        )
    }

    fun mapToTitle(
        rangeLength: RangeLength,
        position: Int,
        startOfDayShift: Long,
        firstDayOfWeek: DayOfWeek,
    ): String {
        return when (rangeLength) {
            is RangeLength.Day -> timeMapper.toDayTitle(position, startOfDayShift)
            is RangeLength.Week -> timeMapper.toWeekTitle(position, startOfDayShift, firstDayOfWeek)
            is RangeLength.Month -> timeMapper.toMonthTitle(position, startOfDayShift)
            is RangeLength.Year -> timeMapper.toYearTitle(position, startOfDayShift)
            is RangeLength.All -> resourceRepo.getString(R.string.range_overall)
            is RangeLength.Custom -> resourceRepo.getString(R.string.range_custom)
            is RangeLength.Last -> resourceRepo.getString(R.string.range_last)
        }
    }

    fun mapToShareTitle(
        rangeLength: RangeLength,
        position: Int,
        startOfDayShift: Long,
        firstDayOfWeek: DayOfWeek,
    ): String {
        return when (rangeLength) {
            is RangeLength.Day -> timeMapper.toDayDateTitle(position, startOfDayShift)
            is RangeLength.Week -> timeMapper.toWeekDateTitle(position, startOfDayShift, firstDayOfWeek)
            is RangeLength.Month -> timeMapper.toMonthDateTitle(position, startOfDayShift)
            is RangeLength.Year -> timeMapper.toYearDateTitle(position, startOfDayShift)
            is RangeLength.All,
            is RangeLength.Custom,
            is RangeLength.Last,
            -> mapToTitle(rangeLength, position, startOfDayShift, firstDayOfWeek)
        }
    }

    fun getRecordsFromRange(
        records: List<Record>,
        rangeStart: Long,
        rangeEnd: Long,
    ): List<Record> {
        return records.filter { it.isInRange(rangeStart, rangeEnd) }
    }

    fun getRunningRecordsFromRange(
        records: List<RunningRecord>,
        rangeStart: Long,
        rangeEnd: Long,
    ): List<RunningRecord> {
        return records.filter { it.timeStarted < rangeEnd && System.currentTimeMillis() > rangeStart }
    }

    fun clampToRange(
        record: Record,
        rangeStart: Long,
        rangeEnd: Long,
    ): Range {
        return Range(
            timeStarted = max(record.timeStarted, rangeStart),
            timeEnded = min(record.timeEnded, rangeEnd)
        )
    }

    fun clampRecordToRange(
        record: Record,
        rangeStart: Long,
        rangeEnd: Long,
    ): Record {
        return if (!record.isCompletelyRange(rangeStart, rangeEnd)) {
            record.copy(
                timeStarted = max(record.timeStarted, rangeStart),
                timeEnded = min(record.timeEnded, rangeEnd)
            )
        } else {
            record
        }
    }

    fun mapToDuration(ranges: List<Range>): Long {
        return ranges.sumOf { it.duration }
    }

    private fun mapToRangeName(rangeLength: RangeLength): RangeViewData {
        val text = when (rangeLength) {
            is RangeLength.Day -> R.string.range_day
            is RangeLength.Week -> R.string.range_week
            is RangeLength.Month -> R.string.range_month
            is RangeLength.Year -> R.string.range_year
            is RangeLength.All -> R.string.range_overall
            is RangeLength.Custom -> R.string.range_custom
            is RangeLength.Last -> R.string.range_last
        }.let(resourceRepo::getString)

        return RangeViewData(
            range = rangeLength,
            text = text
        )
    }

    private fun mapToSelectDateName(rangeLength: RangeLength): SelectDateViewData? {
        return when (rangeLength) {
            is RangeLength.Day -> R.string.range_select_day
            is RangeLength.Week -> R.string.range_select_week
            is RangeLength.Month -> R.string.range_select_month
            is RangeLength.Year -> R.string.range_select_year
            else -> null
        }
            ?.let(resourceRepo::getString)
            ?.let(::SelectDateViewData)
    }

    private fun mapToSelectRange(): SelectRangeViewData {
        return SelectRangeViewData(text = resourceRepo.getString(R.string.range_custom))
    }

    private fun Record.isInRange(
        rangeStart: Long,
        rangeEnd: Long,
    ): Boolean {
        return this.timeStarted < rangeEnd && this.timeEnded > rangeStart
    }

    private fun Record.isCompletelyRange(
        rangeStart: Long,
        rangeEnd: Long,
    ): Boolean {
        return this.timeStarted >= rangeStart && this.timeEnded <= rangeEnd
    }

    companion object {
        private val ranges: List<RangeLength> = listOf(
            RangeLength.Last,
            RangeLength.All,
            RangeLength.Year,
            RangeLength.Month,
            RangeLength.Week,
            RangeLength.Day
        )
    }
}