package com.nextup.backoffice.controller.image

import com.nextup.common.exception.AssociationNotFoundException
import com.nextup.common.exception.EmptyImageFileException
import com.nextup.common.exception.LeagueNotFoundException
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

@DisplayName("ImageAdminController")
class ImageAdminControllerTest {
    private lateinit var imageUploadService: ImageUploadService
    private lateinit var controller: ImageAdminController

    @BeforeEach
    fun setUp() {
        imageUploadService = mockk()
        controller = ImageAdminController(imageUploadService)
    }

    @Nested
    @DisplayName("PUT /api/backoffice/leagues/{leagueId}/logo")
    inner class UploadLeagueLogo {
        @Test
        @DisplayName("리그 로고를 성공적으로 업로드한다")
        fun uploadLeagueLogoSuccess() {
            // given
            val file =
                MockMultipartFile(
                    "file",
                    "logo.png",
                    "image/png",
                    ByteArray(1024) { 0xFF.toByte() },
                )
            every {
                imageUploadService.uploadLeagueLogo(
                    leagueId = 1L,
                    originalFileName = "logo.png",
                    content = any(),
                    contentType = "image/png",
                )
            } returns "/images/leagues/uuid.png"

            // when
            val response = controller.uploadLeagueLogo(1L, file)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.imageUrl).isEqualTo("/images/leagues/uuid.png")
        }

        @Test
        @DisplayName("존재하지 않는 리그이면 예외가 전파된다")
        fun leagueNotFound() {
            val file =
                MockMultipartFile(
                    "file",
                    "logo.png",
                    "image/png",
                    ByteArray(100) { 0xFF.toByte() },
                )
            every {
                imageUploadService.uploadLeagueLogo(
                    leagueId = 999L,
                    originalFileName = "logo.png",
                    content = any(),
                    contentType = "image/png",
                )
            } throws LeagueNotFoundException(999L)

            assertThatThrownBy {
                controller.uploadLeagueLogo(999L, file)
            }.isInstanceOf(LeagueNotFoundException::class.java)
        }

        @Test
        @DisplayName("빈 파일은 EmptyImageFileException을 발생시킨다")
        fun emptyFile() {
            val emptyFile =
                MockMultipartFile(
                    "file",
                    "empty.png",
                    "image/png",
                    ByteArray(0),
                )

            assertThatThrownBy {
                controller.uploadLeagueLogo(1L, emptyFile)
            }.isInstanceOf(EmptyImageFileException::class.java)
        }
    }

    @Nested
    @DisplayName("PUT /api/backoffice/associations/{associationId}/logo")
    inner class UploadAssociationLogo {
        @Test
        @DisplayName("협회 로고를 성공적으로 업로드한다")
        fun uploadAssociationLogoSuccess() {
            // given
            val file =
                MockMultipartFile(
                    "file",
                    "logo.jpg",
                    "image/jpeg",
                    ByteArray(1024) { 0xFF.toByte() },
                )
            every {
                imageUploadService.uploadAssociationLogo(
                    associationId = 1L,
                    originalFileName = "logo.jpg",
                    content = any(),
                    contentType = "image/jpeg",
                )
            } returns "/images/associations/uuid.jpg"

            // when
            val response = controller.uploadAssociationLogo(1L, file)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.imageUrl).isEqualTo("/images/associations/uuid.jpg")
        }

        @Test
        @DisplayName("존재하지 않는 협회이면 예외가 전파된다")
        fun associationNotFound() {
            val file =
                MockMultipartFile(
                    "file",
                    "logo.png",
                    "image/png",
                    ByteArray(100) { 0xFF.toByte() },
                )
            every {
                imageUploadService.uploadAssociationLogo(
                    associationId = 999L,
                    originalFileName = "logo.png",
                    content = any(),
                    contentType = "image/png",
                )
            } throws AssociationNotFoundException(999L)

            assertThatThrownBy {
                controller.uploadAssociationLogo(999L, file)
            }.isInstanceOf(AssociationNotFoundException::class.java)
        }
    }
}
