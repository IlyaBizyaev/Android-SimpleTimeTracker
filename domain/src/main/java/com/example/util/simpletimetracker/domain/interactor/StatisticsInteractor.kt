package com.example.util.simpletimetracker.domain.interactor

import com.example.util.simpletimetracker.domain.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.mapper.CoveredRangeMapper
import com.example.util.simpletimetracker.domain.mapper.StatisticsMapper
import com.example.util.simpletimetracker.domain.model.Range
import com.example.util.simpletimetracker.domain.model.Record
import com.example.util.simpletimetracker.domain.model.Statistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class StatisticsInteractor @Inject constructor(
    private val recordInteractor: RecordInteractor,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val coveredRangeMapper: CoveredRangeMapper,
    private val statisticsMapper: StatisticsMapper
) {

    suspend fun getAllRunning(): List<Statistics> {
        return runningRecordInteractor.getAll()
            .groupBy { it.id }
            .map { entry ->
                Statistics(
                    id = entry.key,
                    duration = entry.value.let(statisticsMapper::mapToRunningDuration)
                )
            }
    }

    suspend fun getFromRange(
        range: Range,
        addUntracked: Boolean,
    ): List<Statistics> = withContext(Dispatchers.IO) {
        val records = getRecords(range)

        records
            .groupBy { it.typeId }
            .let {
                getStatistics(range, it)
            }
            .plus(
                getUntracked(range, records, addUntracked)
            )
    }

    suspend fun getRecords(range: Range): List<Record> {
        return if (rangeIsAllRecords(range)) {
            recordInteractor.getAll()
        } else {
            recordInteractor.getFromRange(range.timeStarted, range.timeEnded)
        }
    }

    fun getStatistics(
        range: Range,
        records: Map<Long, List<Record>>,
    ): List<Statistics> {
        return records.map { (id, records) ->
            Statistics(id, getDuration(range, records))
        }
    }

    fun getDuration(
        range: Range,
        records: List<Record>,
    ): Long {
        // If range is all records - do not clamp to range.
        return if (rangeIsAllRecords(range)) {
            statisticsMapper.mapToDuration(records)
        } else {
            statisticsMapper.mapToDurationFromRange(records, range)
        }
    }

    suspend fun getUntracked(
        range: Range,
        records: List<Record>,
        addUntracked: Boolean,
    ): List<Statistics> {
        if (addUntracked) {
            // If range is all records - calculate from first records to current time.
            val actualRange = if (rangeIsAllRecords(range)) {
                Range(
                    timeStarted = recordInteractor.getNext(0)?.timeStarted.orZero(),
                    timeEnded = System.currentTimeMillis()
                )
            } else {
                range
            }
            val untrackedTime = calculateUntracked(records, actualRange)

            if (untrackedTime > 0L) {
                return Statistics(
                    id = UNTRACKED_ITEM_ID,
                    duration = untrackedTime
                ).let(::listOf)
            }
        }

        return emptyList()
    }

    private fun calculateUntracked(records: List<Record>, range: Range): Long {
        // Bound end range of calculation to current time,
        // to not show untracked time in the future
        val todayEnd = System.currentTimeMillis()

        val untrackedTimeEndRange = min(todayEnd, range.timeEnded)
        if (range.timeStarted > untrackedTimeEndRange) return 0L

        return records
            // Remove parts of the record that are not in the range
            .map { max(it.timeStarted, range.timeStarted) to min(it.timeEnded, untrackedTimeEndRange) }
            // Calculate covered range
            .let(coveredRangeMapper::map)
            // Calculate uncovered range
            .let { untrackedTimeEndRange - range.timeStarted - it }
    }

    private fun rangeIsAllRecords(range: Range): Boolean {
        return range.timeStarted == 0L && range.timeEnded == 0L
    }
}