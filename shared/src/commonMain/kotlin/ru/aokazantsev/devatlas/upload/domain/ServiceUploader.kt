package ru.aokazantsev.devatlas.upload.domain

import ru.aokazantsev.devatlas.upload.domain.model.UploadTarget

interface ServiceUploader {
    suspend fun post(target: UploadTarget, json: String): Int?
}
