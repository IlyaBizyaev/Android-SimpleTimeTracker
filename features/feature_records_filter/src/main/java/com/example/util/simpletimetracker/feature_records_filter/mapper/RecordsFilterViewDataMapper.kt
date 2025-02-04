package com.example.util.simpletimetracker.feature_records_filter.mapper

import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.extension.getCommentItems
import com.example.util.simpletimetracker.domain.extension.getComments
import com.example.util.simpletimetracker.domain.extension.hasAnyComment
import com.example.util.simpletimetracker.domain.extension.hasNoComment
import com.example.util.simpletimetracker.domain.model.RecordsFilter
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.recordFilter.RecordFilterViewData
import com.example.util.simpletimetracker.feature_records_filter.R
import com.example.util.simpletimetracker.navigation.params.screen.RecordsFilterParams
import javax.inject.Inject

class RecordsFilterViewDataMapper @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val timeMapper: TimeMapper,
    private val colorMapper: ColorMapper,
) {

    fun mapRecordsCount(
        extra: RecordsFilterParams,
        count: Int,
        filter: List<RecordsFilter>,
    ): String {
        if (count == 0 && filter.isEmpty()) {
            return extra.title
        }

        val selected = resourceRepo.getString(R.string.something_selected)
        val recordsString: String = resourceRepo.getQuantityString(
            R.plurals.statistics_detail_times_tracked,
            count
        ).lowercase()

        return "$selected $count $recordsString"
    }

    fun mapInactiveFilterName(
        filter: RecordFilterViewData.Type,
    ): String {
        return when (filter) {
            RecordFilterViewData.Type.ACTIVITY -> R.string.activity_hint
            RecordFilterViewData.Type.CATEGORY -> R.string.category_hint
            RecordFilterViewData.Type.COMMENT -> R.string.change_record_comment_field
            RecordFilterViewData.Type.DATE -> R.string.date_time_dialog_date
            RecordFilterViewData.Type.SELECTED_TAGS -> R.string.records_filter_select_tags
            RecordFilterViewData.Type.FILTERED_TAGS -> R.string.records_filter_filter_tags
            RecordFilterViewData.Type.MANUALLY_FILTERED -> R.string.records_filter_manually_filtered
        }.let(resourceRepo::getString)
    }

    fun mapActiveFilterName(
        filter: RecordsFilter,
        useMilitaryTime: Boolean,
    ): String {
        val filterName = filter::class.java
            .let(::mapToViewData)
            ?.let(::mapInactiveFilterName)
            .orEmpty() + " "

        val filterValue = when (filter) {
            is RecordsFilter.Activity -> {
                "${filter.typeIds.size}"
            }
            is RecordsFilter.Category -> {
                "${filter.items.size}"
            }
            is RecordsFilter.Comment -> {
                val items = filter.items
                when {
                    items.hasNoComment() -> {
                        resourceRepo.getString(R.string.records_filter_no_comment)
                    }
                    items.hasAnyComment() -> {
                        resourceRepo.getString(R.string.records_filter_any_comment)
                    }
                    else -> {
                        items.getComments()
                            .firstOrNull()
                            .orEmpty()
                            .replace("\n", " ")
                            .let {
                                if (it.length > 10) it.take(10) + "..." else it
                            }
                    }
                }
            }
            is RecordsFilter.Date -> {
                val startedDate = timeMapper.formatDateTime(
                    time = filter.range.timeStarted,
                    useMilitaryTime = useMilitaryTime,
                    showSeconds = false,
                )
                val endedDate = timeMapper.formatDateTime(
                    time = filter.range.timeEnded,
                    useMilitaryTime = useMilitaryTime,
                    showSeconds = false,
                )
                "$startedDate - $endedDate"
            }
            is RecordsFilter.SelectedTags -> {
                "${filter.items.size}"
            }
            is RecordsFilter.FilteredTags -> {
                "${filter.items.size}"
            }
            is RecordsFilter.ManuallyFiltered -> {
                "${filter.recordIds.size}"
            }
        }

        return "$filterName($filterValue)"
    }

    fun mapCommentFilter(
        type: RecordFilterViewData.CommentType,
        filters: List<RecordsFilter>,
        isDarkTheme: Boolean,
    ): ViewHolderType {
        val name: String
        val enabled: Boolean

        when (type) {
            RecordFilterViewData.CommentType.NO_COMMENT -> {
                enabled = filters.getCommentItems().hasNoComment()
                name = resourceRepo.getString(R.string.records_filter_no_comment)
            }
            RecordFilterViewData.CommentType.ANY_COMMENT -> {
                enabled = filters.getCommentItems().hasAnyComment()
                name = resourceRepo.getString(R.string.records_filter_any_comment)
            }
        }

        return RecordFilterViewData(
            id = type.ordinal.toLong(),
            type = RecordFilterViewData.Type.COMMENT,
            name = name,
            color = if (enabled) {
                colorMapper.toActiveColor(isDarkTheme)
            } else {
                colorMapper.toInactiveColor(isDarkTheme)
            },
            selected = enabled,
            removeBtnVisible = false,
        )
    }

    fun mapToClass(type: RecordFilterViewData.Type): Class<out RecordsFilter> {
        return when (type) {
            RecordFilterViewData.Type.ACTIVITY -> RecordsFilter.Activity::class.java
            RecordFilterViewData.Type.CATEGORY -> RecordsFilter.Category::class.java
            RecordFilterViewData.Type.COMMENT -> RecordsFilter.Comment::class.java
            RecordFilterViewData.Type.DATE -> RecordsFilter.Date::class.java
            RecordFilterViewData.Type.SELECTED_TAGS -> RecordsFilter.SelectedTags::class.java
            RecordFilterViewData.Type.FILTERED_TAGS -> RecordsFilter.FilteredTags::class.java
            RecordFilterViewData.Type.MANUALLY_FILTERED -> RecordsFilter.ManuallyFiltered::class.java
        }
    }

    private fun mapToViewData(clazz: Class<out RecordsFilter>): RecordFilterViewData.Type? {
        return when (clazz) {
            RecordsFilter.Activity::class.java -> RecordFilterViewData.Type.ACTIVITY
            RecordsFilter.Category::class.java -> RecordFilterViewData.Type.CATEGORY
            RecordsFilter.Comment::class.java -> RecordFilterViewData.Type.COMMENT
            RecordsFilter.Date::class.java -> RecordFilterViewData.Type.DATE
            RecordsFilter.SelectedTags::class.java -> RecordFilterViewData.Type.SELECTED_TAGS
            RecordsFilter.FilteredTags::class.java -> RecordFilterViewData.Type.FILTERED_TAGS
            RecordsFilter.ManuallyFiltered::class.java -> RecordFilterViewData.Type.MANUALLY_FILTERED
            else -> null
        }
    }
}