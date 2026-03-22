package com.nextup.infrastructure.service.image

import com.nextup.common.exception.AssociationNotFoundException
import com.nextup.common.exception.EmptyImageFileException
import com.nextup.common.exception.ImageFileSizeExceededException
import com.nextup.common.exception.LeagueNotFoundException
import com.nextup.common.exception.PlayerNotLinkedException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.common.exception.UnsupportedImageFormatException
import com.nextup.common.exception.UserNotFoundException
import com.nextup.core.port.repository.AssociationRepositoryPort
import com.nextup.core.port.repository.LeagueRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
import com.nextup.core.port.service.ImageStoragePort
import com.nextup.core.service.image.ImageUploadService
import com.nextup.infrastructure.config.ImageUploadProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 이미지 업로드 서비스 구현체
 *
 * 파일 검증(크기, MIME 타입) 후 저장소에 저장하고,
 * 엔티티의 logoUrl/profileImageUrl 필드를 업데이트합니다.
 */
@Service
@Transactional(readOnly = true)
class ImageUploadServiceImpl(
    private val imageStoragePort: ImageStoragePort,
    private val properties: ImageUploadProperties,
    private val teamRepository: TeamRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val leagueRepository: LeagueRepositoryPort,
    private val associationRepository: AssociationRepositoryPort,
) : ImageUploadService {
    override fun uploadImage(
        directory: String,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String {
        validateImage(content, contentType)
        val storedFileName = generateFileName(originalFileName)
        return imageStoragePort.store(directory, storedFileName, content, contentType)
    }

    @Transactional
    override fun uploadTeamLogo(
        teamId: Long,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String {
        validateImage(content, contentType)

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw TeamNotFoundException(teamId)

        val oldLogoUrl = team.logoUrl
        val storedFileName = generateFileName(originalFileName)
        val imageUrl = imageStoragePort.store("teams", storedFileName, content, contentType)

        team.updateInfo(logoUrl = imageUrl)
        teamRepository.save(team)

        oldLogoUrl?.let { deleteOldImageSafely(it) }

        return imageUrl
    }

    @Transactional
    override fun uploadPlayerProfileImage(
        userId: Long,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String {
        validateImage(content, contentType)

        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw UserNotFoundException(userId)

        val player =
            user.player
                ?: throw PlayerNotLinkedException(userId)

        val oldImageUrl = player.profileImageUrl
        val storedFileName = generateFileName(originalFileName)
        val imageUrl = imageStoragePort.store("players", storedFileName, content, contentType)

        player.updateProfile(imageUrl)
        playerRepository.save(player)

        oldImageUrl?.let { deleteOldImageSafely(it) }

        return imageUrl
    }

    @Transactional
    override fun uploadLeagueLogo(
        leagueId: Long,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String {
        validateImage(content, contentType)

        val league =
            leagueRepository.findByIdOrNull(leagueId)
                ?: throw LeagueNotFoundException(leagueId)

        val oldLogoUrl = league.logoUrl
        val storedFileName = generateFileName(originalFileName)
        val imageUrl = imageStoragePort.store("leagues", storedFileName, content, contentType)

        league.updateInfo(logoUrl = imageUrl)
        leagueRepository.save(league)

        oldLogoUrl?.let { deleteOldImageSafely(it) }

        return imageUrl
    }

    @Transactional
    override fun uploadAssociationLogo(
        associationId: Long,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String {
        validateImage(content, contentType)

        val association =
            associationRepository.findByIdOrNull(associationId)
                ?: throw AssociationNotFoundException(associationId)

        val oldLogoUrl = association.logoUrl
        val storedFileName = generateFileName(originalFileName)
        val imageUrl = imageStoragePort.store("associations", storedFileName, content, contentType)

        association.updateInfo(logoUrl = imageUrl)
        associationRepository.save(association)

        oldLogoUrl?.let { deleteOldImageSafely(it) }

        return imageUrl
    }

    private fun validateImage(
        content: ByteArray,
        contentType: String,
    ) {
        if (content.isEmpty()) {
            throw EmptyImageFileException()
        }
        if (content.size.toLong() > properties.maxFileSize) {
            throw ImageFileSizeExceededException(content.size.toLong(), properties.maxFileSize)
        }
        if (contentType !in properties.allowedContentTypes) {
            throw UnsupportedImageFormatException(contentType)
        }
    }

    private fun generateFileName(originalFileName: String): String {
        val extension =
            originalFileName.substringAfterLast('.', "png").lowercase()
        return "${UUID.randomUUID()}.$extension"
    }

    private fun deleteOldImageSafely(imageUrl: String) {
        try {
            imageStoragePort.delete(imageUrl)
        } catch (e: Exception) {
            // 기존 이미지 삭제 실패는 무시 (로깅만)
            org.slf4j.LoggerFactory.getLogger(javaClass)
                .warn("기존 이미지 삭제 실패 (무시): {}", imageUrl, e)
        }
    }
}
