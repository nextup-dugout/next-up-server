package com.nextup.api.controller.image

import com.nextup.common.exception.EmptyImageFileException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.common.exception.UnsupportedImageFormatException
import com.nextup.core.service.image.ImageUploadService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

@DisplayName("ImageController")
class ImageControllerTest {
    private lateinit var imageUploadService: ImageUploadService
    private lateinit var controller: ImageController

    @BeforeEach
    fun setUp() {
        imageUploadService = mockk()
        controller = ImageController(imageUploadService)
    }

    @Nested
    @DisplayName("POST /api/v1/images/upload")
    inner class UploadImage {
        @Test
        @DisplayName("이미지를 성공적으로 업로드한다")
        fun uploadSuccess() {
            // given
            val file =
                MockMultipartFile(
                    "file",
                    "test.png",
                    "image/png",
                    ByteArray(1024) { 0xFF.toByte() },
                )
            every {
                imageUploadService.uploadImage(
                    directory = "general",
                    originalFileName = "test.png",
                    content = any(),
                    contentType = "image/png",
                )
            } returns "/images/general/uuid.png"

            // when
            val response = controller.uploadImage(file, "general", 100L)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.imageUrl).isEqualTo("/images/general/uuid.png")
        }

        @Test
        @DisplayName("빈 파일은 EmptyImageFileException을 발생시킨다")
        fun uploadEmptyFile() {
            val emptyFile =
                MockMultipartFile(
                    "file",
                    "empty.png",
                    "image/png",
                    ByteArray(0),
                )

            assertThatThrownBy {
                controller.uploadImage(emptyFile, "general", 100L)
            }.isInstanceOf(EmptyImageFileException::class.java)
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/teams/{teamId}/logo")
    inner class UploadTeamLogo {
        @Test
        @DisplayName("팀 로고를 성공적으로 업로드한다")
        fun uploadTeamLogoSuccess() {
            // given
            val file =
                MockMultipartFile(
                    "file",
                    "logo.jpg",
                    "image/jpeg",
                    ByteArray(1024) { 0xFF.toByte() },
                )
            every {
                imageUploadService.uploadTeamLogo(
                    teamId = 1L,
                    originalFileName = "logo.jpg",
                    content = any(),
                    contentType = "image/jpeg",
                )
            } returns "/images/teams/uuid.jpg"

            // when
            val response = controller.uploadTeamLogo(1L, file, 100L)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.imageUrl).isEqualTo("/images/teams/uuid.jpg")
        }

        @Test
        @DisplayName("존재하지 않는 팀이면 예외가 전파된다")
        fun teamNotFound() {
            val file =
                MockMultipartFile(
                    "file",
                    "logo.png",
                    "image/png",
                    ByteArray(100) { 0xFF.toByte() },
                )
            every {
                imageUploadService.uploadTeamLogo(
                    teamId = 999L,
                    originalFileName = "logo.png",
                    content = any(),
                    contentType = "image/png",
                )
            } throws TeamNotFoundException(999L)

            assertThatThrownBy {
                controller.uploadTeamLogo(999L, file, 100L)
            }.isInstanceOf(TeamNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/players/me/profile-image")
    inner class UploadPlayerProfileImage {
        @Test
        @DisplayName("선수 프로필 이미지를 성공적으로 업로드한다")
        fun uploadProfileSuccess() {
            // given
            val file =
                MockMultipartFile(
                    "file",
                    "profile.webp",
                    "image/webp",
                    ByteArray(1024) { 0xFF.toByte() },
                )
            every {
                imageUploadService.uploadPlayerProfileImage(
                    userId = 100L,
                    originalFileName = "profile.webp",
                    content = any(),
                    contentType = "image/webp",
                )
            } returns "/images/players/uuid.webp"

            // when
            val response = controller.uploadPlayerProfileImage(file, 100L)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.imageUrl).isEqualTo("/images/players/uuid.webp")
        }

        @Test
        @DisplayName("서비스에서 예외가 발생하면 전파된다")
        fun serviceExceptionPropagates() {
            val file =
                MockMultipartFile(
                    "file",
                    "test.gif",
                    "image/gif",
                    ByteArray(100) { 0xFF.toByte() },
                )
            every {
                imageUploadService.uploadPlayerProfileImage(
                    userId = 100L,
                    originalFileName = "test.gif",
                    content = any(),
                    contentType = "image/gif",
                )
            } throws UnsupportedImageFormatException("image/gif")

            assertThatThrownBy {
                controller.uploadPlayerProfileImage(file, 100L)
            }.isInstanceOf(UnsupportedImageFormatException::class.java)
        }
    }
}
