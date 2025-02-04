package com.example.util.simpletimetracker.feature_running_records.interactor

import com.example.util.simpletimetracker.core.interactor.ActivityFilterViewDataInteractor
import com.example.util.simpletimetracker.core.interactor.GetCurrentRecordsDurationInteractor
import com.example.util.simpletimetracker.core.mapper.RunningRecordViewDataMapper
import com.example.util.simpletimetracker.domain.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.divider.DividerViewData
import com.example.util.simpletimetracker.feature_running_records.mapper.RunningRecordsViewDataMapper
import javax.inject.Inject

class RunningRecordsViewDataInteractor @Inject constructor(
    private val prefsInteractor: PrefsInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val activityFilterViewDataInteractor: ActivityFilterViewDataInteractor,
    private val getCurrentRecordsDurationInteractor: GetCurrentRecordsDurationInteractor,
    private val mapper: RunningRecordsViewDataMapper,
    private val runningRecordViewDataMapper: RunningRecordViewDataMapper,
) {

    suspend fun getViewData(): List<ViewHolderType> {
        val recordTypes = recordTypeInteractor.getAll()
        val recordTypesMap = recordTypes.associateBy { it.id }
        val recordTags = recordTagInteractor.getAll()
        val runningRecords = runningRecordInteractor.getAll()
        val recordTypesRunning = runningRecords.map { it.id }
        val numberOfCards = prefsInteractor.getNumberOfCards()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat()
        val showSeconds = prefsInteractor.getShowSeconds()

        val runningRecordsViewData = when {
            recordTypes.filterNot { it.hidden }.isEmpty() ->
                listOf(mapper.mapToTypesEmpty())
            runningRecords.isEmpty() ->
                listOf(mapper.mapToEmpty())
            else -> {
                runningRecords
                    .sortedByDescending {
                        it.timeStarted
                    }
                    .mapNotNull { runningRecord ->
                        val recordType = recordTypesMap[runningRecord.id] ?: return@mapNotNull null
                        val dailyCurrent = if (recordType.dailyGoalTime > 0L) {
                            getCurrentRecordsDurationInteractor.getDailyCurrent(runningRecord)
                        } else {
                            0L
                        }
                        val weeklyCurrent = if (recordType.weeklyGoalTime > 0L) {
                            getCurrentRecordsDurationInteractor.getWeeklyCurrent(runningRecord)
                        } else {
                            0L
                        }
                        val monthlyCurrent = if (recordType.monthlyGoalTime > 0L) {
                            getCurrentRecordsDurationInteractor.getMonthlyCurrent(runningRecord)
                        } else {
                            0L
                        }

                        runningRecordViewDataMapper.map(
                            runningRecord = runningRecord,
                            dailyCurrent = dailyCurrent,
                            weeklyCurrent = weeklyCurrent,
                            monthlyCurrent = monthlyCurrent,
                            recordType = recordType,
                            recordTags = recordTags.filter { it.id in runningRecord.tagIds },
                            isDarkTheme = isDarkTheme,
                            useMilitaryTime = useMilitaryTime,
                            showSeconds = showSeconds,
                            nowIconVisible = false,
                        )
                    }
                    .plus(
                        mapper.mapToHasRunningRecords()
                    )
            }
        }

        val filter = activityFilterViewDataInteractor.getFilter()
        val filtersViewData = activityFilterViewDataInteractor.getFilterViewData(
            filter = filter,
            isDarkTheme = isDarkTheme,
            appendAddButton = true,
        ).let {
            if (it.isNotEmpty()) it + DividerViewData(2) else it
        }

        val recordTypesViewData = recordTypes
            .filterNot {
                it.hidden
            }
            .let { list ->
                activityFilterViewDataInteractor.applyFilter(list, filter)
            }
            .map {
                mapper.map(
                    recordType = it,
                    isFiltered = it.id in recordTypesRunning,
                    numberOfCards = numberOfCards,
                    isDarkTheme = isDarkTheme
                )
            }
            .plus(
                mapper.mapToAddItem(
                    numberOfCards = numberOfCards,
                    isDarkTheme = isDarkTheme
                )
            )
            .let {
                if (recordTypes.isEmpty()) {
                    it + mapper.mapToAddDefaultItem(
                        numberOfCards = numberOfCards,
                        isDarkTheme = isDarkTheme
                    )
                } else {
                    it
                }
            }

        return runningRecordsViewData +
            listOf(DividerViewData(1)) +
            filtersViewData +
            recordTypesViewData
    }
}