package com.example.util.simpletimetracker.feature_statistics.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.adapter.ViewHolderType
import com.example.util.simpletimetracker.core.adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.interactor.StatisticsInteractor
import com.example.util.simpletimetracker.feature_statistics.extra.StatisticsExtra
import com.example.util.simpletimetracker.feature_statistics.mapper.StatisticsViewDataMapper
import com.example.util.simpletimetracker.feature_statistics.viewData.RangeLength
import com.example.util.simpletimetracker.feature_statistics.viewData.StatisticsViewData
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.Screen
import com.example.util.simpletimetracker.navigation.params.StatisticsDetailParams
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

class StatisticsViewModel @Inject constructor(
    private val router: Router,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val statisticsInteractor: StatisticsInteractor,
    private val prefsInteractor: PrefsInteractor,
    private val statisticsViewDataMapper: StatisticsViewDataMapper
) : ViewModel() {

    var extra: StatisticsExtra? = null

    val statistics: LiveData<List<ViewHolderType>> by lazy {
        MutableLiveData(listOf(LoaderViewData() as ViewHolderType))
    }

    private var rangeLength: RangeLength? = null
    private var start: Long = 0
    private var end: Long = 0

    fun onVisible() {
        updateStatistics()
    }

    fun onNewRange(newRangeLength: RangeLength) {
        rangeLength = newRangeLength
        getRange().let { (start, end) ->
            this.start = start
            this.end = end
        }
        updateStatistics()
    }

    fun onFilterClick() {
        router.navigate(Screen.CHART_FILTER_DIALOG)
    }

    fun onItemClick(item: StatisticsViewData, sharedElements: Map<Any, String>) {
        if (item.typeId == -1L) return // TODO untracked detailed statistics

        router.navigate(
            screen = Screen.STATISTICS_DETAIL,
            data = StatisticsDetailParams(item.typeId),
            sharedElements = sharedElements
        )
    }

    fun onFilterApplied() {
        updateStatistics()
    }

    private fun getRange(): Pair<Long, Long> {
        val shift = extra?.shift.orZero()
        val rangeStart: Long
        val rangeEnd: Long
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (rangeLength) {
            RangeLength.DAY -> {
                calendar.add(Calendar.DATE, shift)
                rangeStart = calendar.timeInMillis
                rangeEnd = calendar.apply { add(Calendar.DATE, 1) }.timeInMillis
            }
            RangeLength.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.add(Calendar.DATE, shift * 7)
                rangeStart = calendar.timeInMillis
                rangeEnd = calendar.apply { add(Calendar.DATE, 7) }.timeInMillis
            }
            RangeLength.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MONTH, shift)
                rangeStart = calendar.timeInMillis
                rangeEnd = calendar.apply { add(Calendar.MONTH, 1) }.timeInMillis
            }
            RangeLength.ALL -> {
                rangeStart = 0L
                rangeEnd = 0L
            }
            else -> {
                rangeStart = 0L
                rangeEnd = 0L
            }
        }

        return rangeStart to rangeEnd
    }

    private fun updateStatistics() = viewModelScope.launch {
        val data = loadStatisticsViewData()
        (statistics as MutableLiveData).value = data
    }

    private suspend fun loadStatisticsViewData(): List<ViewHolderType> {
        val showDuration: Boolean
        val types = recordTypeInteractor.getAll()
        val typesFiltered = prefsInteractor.getFilteredTypes()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val statistics = if (start.orZero() != 0L && end.orZero() != 0L) {
            showDuration = true
            statisticsInteractor.getFromRange(
                start = start.orZero(),
                end = end.orZero(),
                addUntracked = !typesFiltered.contains(-1L)
            )
        } else {
            showDuration = false
            statisticsInteractor.getAll()
        }

        val list = statisticsViewDataMapper.map(statistics, types, typesFiltered, showDuration, isDarkTheme)
        val chart = statisticsViewDataMapper.mapToChart(statistics, types, typesFiltered, isDarkTheme)

        if (list.isEmpty()) return listOf(statisticsViewDataMapper.mapToEmpty())
        return listOf(chart) + list
    }
}
