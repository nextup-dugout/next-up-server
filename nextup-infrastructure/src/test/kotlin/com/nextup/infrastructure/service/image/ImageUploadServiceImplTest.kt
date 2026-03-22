package com.nextup.infrastructure.service.image

import com.nextup.common.exception.AssociationNotFoundException
import com.nextup.common.exception.EmptyImageFileException
import com.nextup.common.exception.ImageFileSizeExceededException
import com.nextup.common.exception.LeagueNotFoundException
import com.nextup.common.exception.PlayerNotLinkedException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.common.exception.UnsupportedImageFormatException
import com.nextup.common.exception.UserNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.AssociationRepositoryPort
import com.nextup.core.port.repository.LeagueRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
import com.nextup.core.port.service.ImageStoragePort
import com.nextup.infrastructure.config.ImageUploadProperties
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ImageUploadServiceImpl")
class ImageUploadServiceImplTest {
    private val imageStoragePort: ImageStoragePort = mockk()
    private val teamRepository: TeamRepositoryPort = mockk()
    private val playerRepository: PlayerRepositoryPort = mockk()
    private val userRepository: UserRepositoryPort = mockk()
    private val leagueRepository: LeagueRepositoryPort = mockk()
    private val associationRepository: AssociationRepositoryPort = mockk()

    private val properties =
        ImageUploadProperties(
            maxFileSize = 5 * 1024 * 1024,
            allowedContentTypes = setOf("image/jpeg", "image/png", "image/webp"),
            storagePath = "uploads/images",
            baseUrl = "/images",
        )

    private lateinit var service: ImageUploadServiceImpl

    @BeforeEach
    fun setUp() {
        service =
            ImageUploadServiceImpl(
                imageStoragePort = imageStoragePort,
                properties = properties,
                teamRepository = teamRepository,
                playerRepository = playerRepository,
                userRepository = userRepository,
                leagueRepository = leagueRepository,
                associationRepository = associationRepository,
            )
    }

    @Nested
    @DisplayName("uploadImage")
    inner class UploadImage {
        @Test
        @DisplayName("범용 이미지를 성공적으로 업로드한다")
        fun uploadImageSuccess() {
            // given
            val content = ByteArray(1024) { 0xFF.toByte() }
            every { imageStoragePort.store("general", any(), content, "image/png") } returns "/images/general/test.png"

            // when
            val result = service.uploadImage("general", "test.png", content, "image/png")

            // then
            assertThat(result).isEqualTo("/images/general/test.png")
            verify { imageStoragePort.store("general", any(), content, "image/png") }
        }

        @Test
        @DisplayName("빈 파일은 EmptyImageFileException을 발생시킨다")
        fun uploadEmptyImage() {
            assertThatThrownBy {
                service.uploadImage("general", "test.png", ByteArray(0), "image/png")
            }.isInstanceOf(EmptyImageFileException::class.java)
        }

        @Test
        @DisplayName("파일 크기 초과 시 ImageFileSizeExceededException을 발생시킨다")
        fun uploadOversizedImage() {
            val oversized = ByteArray(6 * 1024 * 1024) { 0xFF.toByte() }
            assertThatThrownBy {
                service.uploadImage("general", "big.png", oversized, "image/png")
            }.isInstanceOf(ImageFileSizeExceededException::class.java)
        }

        @Test
        @DisplayName("지원하지 않는 MIME 타입은 UnsupportedImageFormatException을 발생시킨다")
        fun uploadUnsupportedFormat() {
            val content = ByteArray(100) { 0xFF.toByte() }
            assertThatThrownBy {
                service.uploadImage("general", "test.gif", content, "image/gif")
            }.isInstanceOf(UnsupportedImageFormatException::class.java)
        }
    }

    @Nested
    @DisplayName("uploadTeamLogo")
    inner class UploadTeamLogo {
        @Test
        @DisplayName("팀 로고를 성공적으로 업로드한다")
        fun uploadTeamLogoSuccess() {
            // given
            val content = ByteArray(1024) { 0xFF.toByte() }
            val team =
                mockk<Team> {
                    every { logoUrl } returns null
                    every { primaryColor } returns null
                    every { secondaryColor } returns null
                    every { updateInfo(any(), any(), any()) } returns Unit
                }
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamRepository.save(team) } returns team
            every { imageStoragePort.store("teams", any(), content, "image/jpeg") } returns "/images/teams/uuid.jpg"

            // when
            val result = service.uploadTeamLogo(1L, "logo.jpg", content, "image/jpeg")

            // then
            assertThat(result).isEqualTo("/images/teams/uuid.jpg")
            verify { team.updateInfo(any(), any(), any()) }
            verify { teamRepository.save(team) }
        }

        @Test
        @DisplayName("존재하지 않는 팀이면 TeamNotFoundException을 발생시킨다")
        fun uploadTeamLogoNotFound() {
            val content = ByteArray(100) { 0xFF.toByte() }
            every { teamRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy {
                service.uploadTeamLogo(999L, "logo.png", content, "image/png")
            }.isInstanceOf(TeamNotFoundException::class.java)
        }

        @Test
        @DisplayName("기존 로고가 있으면 삭제 후 새 로고를 설정한다")
        fun uploadTeamLogoReplacesOld() {
            // given
            val content = ByteArray(1024) { 0xFF.toByte() }
            val team =
                mockk<Team> {
                    every { logoUrl } returns "/images/teams/old.png"
                    every { primaryColor } returns null
                    every { secondaryColor } returns null
                    every { updateInfo(any(), any(), any()) } returns Unit
                }
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamRepository.save(team) } returns team
            every { imageStoragePort.store("teams", any(), content, "image/png") } returns "/images/teams/new.png"
            justRun { imageStoragePort.delete("/images/teams/old.png") }

            // when
            val result = service.uploadTeamLogo(1L, "logo.png", content, "image/png")

            // then
            assertThat(result).isEqualTo("/images/teams/new.png")
            verify { imageStoragePort.delete("/images/teams/old.png") }
        }
    }

    @Nested
    @DisplayName("uploadPlayerProfileImage")
    inner class UploadPlayerProfileImage {
        @Test
        @DisplayName("선수 프로필 이미지를 성공적으로 업로드한다")
        fun uploadPlayerProfileSuccess() {
            // given
            val content = ByteArray(1024) { 0xFF.toByte() }
            val player =
                mockk<Player> {
                    every { profileImageUrl } returns null
                    every { updateProfile(any()) } returns Unit
                }
            val user =
                mockk<User> {
                    every { this@mockk.player } returns player
                }
            every { userRepository.findByIdOrNull(100L) } returns user
            every { playerRepository.save(player) } returns player
            every { imageStoragePort.store("players", any(), content, "image/png") } returns "/images/players/uuid.png"

            // when
            val result = service.uploadPlayerProfileImage(100L, "profile.png", content, "image/png")

            // then
            assertThat(result).isEqualTo("/images/players/uuid.png")
            verify { player.updateProfile("/images/players/uuid.png") }
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 UserNotFoundException을 발생시킨다")
        fun userNotFound() {
            val content = ByteArray(100) { 0xFF.toByte() }
            every { userRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy {
                service.uploadPlayerProfileImage(999L, "profile.png", content, "image/png")
            }.isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        @DisplayName("연결된 선수가 없으면 PlayerNotLinkedException을 발생시킨다")
        fun playerNotLinked() {
            val content = ByteArray(100) { 0xFF.toByte() }
            val user =
                mockk<User> {
                    every { player } returns null
                }
            every { userRepository.findByIdOrNull(100L) } returns user

            assertThatThrownBy {
                service.uploadPlayerProfileImage(100L, "profile.png", content, "image/png")
            }.isInstanceOf(PlayerNotLinkedException::class.java)
        }
    }

    @Nested
    @DisplayName("uploadLeagueLogo")
    inner class UploadLeagueLogo {
        @Test
        @DisplayName("리그 로고를 성공적으로 업로드한다")
        fun uploadLeagueLogoSuccess() {
            // given
            val content = ByteArray(1024) { 0xFF.toByte() }
            val league =
                mockk<League> {
                    every { logoUrl } returns null
                    every { description } returns null
                    every { updateInfo(any(), any()) } returns Unit
                }
            every { leagueRepository.findByIdOrNull(1L) } returns league
            every { leagueRepository.save(league) } returns league
            every { imageStoragePort.store("leagues", any(), content, "image/png") } returns "/images/leagues/uuid.png"

            // when
            val result = service.uploadLeagueLogo(1L, "logo.png", content, "image/png")

            // then
            assertThat(result).isEqualTo("/images/leagues/uuid.png")
            verify { league.updateInfo(any(), any()) }
        }

        @Test
        @DisplayName("존재하지 않는 리그이면 LeagueNotFoundException을 발생시킨다")
        fun leagueNotFound() {
            val content = ByteArray(100) { 0xFF.toByte() }
            every { leagueRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy {
                service.uploadLeagueLogo(999L, "logo.png", content, "image/png")
            }.isInstanceOf(LeagueNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("uploadAssociationLogo")
    inner class UploadAssociationLogo {
        @Test
        @DisplayName("협회 로고를 성공적으로 업로드한다")
        fun uploadAssociationLogoSuccess() {
            // given
            val content = ByteArray(1024) { 0xFF.toByte() }
            val association =
                mockk<Association> {
                    every { logoUrl } returns null
                    every { description } returns null
                    every { websiteUrl } returns null
                    every { updateInfo(any(), any(), any()) } returns Unit
                }
            every { associationRepository.findByIdOrNull(1L) } returns association
            every { associationRepository.save(association) } returns association
            every {
                imageStoragePort.store("associations", any(), content, "image/png")
            } returns "/images/associations/uuid.png"

            // when
            val result = service.uploadAssociationLogo(1L, "logo.png", content, "image/png")

            // then
            assertThat(result).isEqualTo("/images/associations/uuid.png")
            verify { association.updateInfo(any(), any(), any()) }
        }

        @Test
        @DisplayName("존재하지 않는 협회이면 AssociationNotFoundException을 발생시킨다")
        fun associationNotFound() {
            val content = ByteArray(100) { 0xFF.toByte() }
            every { associationRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy {
                service.uploadAssociationLogo(999L, "logo.png", content, "image/png")
            }.isInstanceOf(AssociationNotFoundException::class.java)
        }
    }
}
