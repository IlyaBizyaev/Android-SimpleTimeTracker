package com.example.util.simpletimetracker.domain.interactor

import com.example.util.simpletimetracker.domain.model.Record
import com.example.util.simpletimetracker.domain.repo.RecordRepo
import com.example.util.simpletimetracker.domain.repo.RecordToRecordTagRepo
import javax.inject.Inject

class RecordInteractor @Inject constructor(
    private val recordRepo: RecordRepo,
    private val recordToRecordTagRepo: RecordToRecordTagRepo,
) {

    suspend fun getAll(): List<Record> {
        return recordRepo.getAll()
    }

    suspend fun getByType(typeIds: List<Long>): List<Record> {
        return recordRepo.getByType(typeIds)
    }

    suspend fun getByTypeWithAnyComment(typeIds: List<Long>): List<Record> {
        return recordRepo.getByTypeWithAnyComment(typeIds)
    }

    suspend fun searchComment(text: String): List<Record> {
        return recordRepo.searchComment(text)
    }

    suspend fun searchByTypeWithComment(typeIds: List<Long>, text: String): List<Record> {
        return recordRepo.searchByTypeWithComment(typeIds, text)
    }

    suspend fun searchAnyComments(): List<Record> {
        return recordRepo.searchAnyComments()
    }

    suspend fun get(id: Long): Record? {
        return recordRepo.get(id)
    }

    suspend fun getPrev(timeStarted: Long): Record? {
        return recordRepo.getPrev(timeStarted)
    }

    suspend fun getNext(timeEnded: Long): Record? {
        return recordRepo.getNext(timeEnded)
    }

    suspend fun getFromRange(start: Long, end: Long): List<Record> {
        return recordRepo.getFromRange(start, end)
    }

    suspend fun getFromRangeByType(typeIds: List<Long>, start: Long, end: Long): List<Record> {
        return recordRepo.getFromRangeByType(typeIds, start, end)
    }

    suspend fun add(
        typeId: Long,
        timeStarted: Long,
        comment: String,
        tagIds: List<Long>,
    ) {
        Record(
            typeId = typeId,
            timeStarted = timeStarted,
            timeEnded = System.currentTimeMillis(),
            comment = comment,
            tagIds = tagIds,
        ).let {
            add(it)
        }
    }

    suspend fun add(record: Record) {
        val recordId = recordRepo.add(record)
        recordToRecordTagRepo.removeAllByRecordId(recordId)
        recordToRecordTagRepo.addRecordTags(recordId, record.tagIds)
    }

    suspend fun remove(id: Long) {
        recordToRecordTagRepo.removeAllByRecordId(id)
        recordRepo.remove(id)
    }

    suspend fun clear() {
        recordRepo.clear()
    }
}