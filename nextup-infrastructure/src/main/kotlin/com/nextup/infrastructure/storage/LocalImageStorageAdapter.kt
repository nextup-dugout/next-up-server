package com.nextup.infrastructure.storage

import com.nextup.common.exception.ImageStorageException
import com.nextup.core.port.service.ImageStoragePort
import com.nextup.infrastructure.config.ImageUploadProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * 로컬 파일시스템 기반 이미지 저장소 어댑터
 *
 * 개발 환경용 구현체입니다. 운영 환경에서는 S3/MinIO 기반 구현체로 교체합니다.
 */
@Component
class LocalImageStorageAdapter(
    private val properties: ImageUploadProperties,
) : ImageStoragePort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun store(
        directory: String,
        fileName: String,
        content: ByteArray,
        contentType: String,
    ): String {
        try {
            val dirPath: Path = Paths.get(properties.storagePath, directory)
            Files.createDirectories(dirPath)

            val filePath = dirPath.resolve(fileName)
            Files.write(
                filePath,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
            )

            log.info("이미지 저장 완료: {} ({} bytes)", filePath, content.size)
            return "${properties.baseUrl}/$directory/$fileName"
        } catch (e: Exception) {
            log.error("이미지 저장 실패: {}/{}", directory, fileName, e)
            throw ImageStorageException("이미지 저장에 실패했습니다: ${e.message}")
        }
    }

    override fun delete(imageUrl: String) {
        try {
            val relativePath = imageUrl.removePrefix(properties.baseUrl).trimStart('/')
            val filePath = Paths.get(properties.storagePath, relativePath)

            if (Files.exists(filePath)) {
                Files.delete(filePath)
                log.info("이미지 삭제 완료: {}", filePath)
            } else {
                log.warn("삭제할 이미지 파일이 존재하지 않습니다: {}", filePath)
            }
        } catch (e: Exception) {
            log.error("이미지 삭제 실패: {}", imageUrl, e)
            throw ImageStorageException("이미지 삭제에 실패했습니다: ${e.message}")
        }
    }
}
