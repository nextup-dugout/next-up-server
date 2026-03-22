package com.nextup.infrastructure.storage

import com.nextup.infrastructure.config.ImageUploadProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("LocalImageStorageAdapter")
class LocalImageStorageAdapterTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var adapter: LocalImageStorageAdapter
    private lateinit var properties: ImageUploadProperties

    @BeforeEach
    fun setUp() {
        properties =
            ImageUploadProperties(
                storagePath = tempDir.toString(),
                baseUrl = "/images",
            )
        adapter = LocalImageStorageAdapter(properties)
    }

    @Nested
    @DisplayName("store")
    inner class Store {
        @Test
        @DisplayName("이미지를 성공적으로 저장하고 URL을 반환한다")
        fun storeSuccess() {
            // given
            val content = "fake image data".toByteArray()

            // when
            val url = adapter.store("teams", "test.png", content, "image/png")

            // then
            assertThat(url).isEqualTo("/images/teams/test.png")
            val storedFile = tempDir.resolve("teams").resolve("test.png")
            assertThat(Files.exists(storedFile)).isTrue()
            assertThat(Files.readAllBytes(storedFile)).isEqualTo(content)
        }

        @Test
        @DisplayName("하위 디렉토리를 자동 생성한다")
        fun createsDirectories() {
            // given
            val content = "data".toByteArray()

            // when
            adapter.store("players", "profile.jpg", content, "image/jpeg")

            // then
            assertThat(Files.isDirectory(tempDir.resolve("players"))).isTrue()
        }

        @Test
        @DisplayName("동일 파일명으로 저장 시 덮어쓴다")
        fun overwritesExisting() {
            // given
            val originalContent = "original".toByteArray()
            val newContent = "updated".toByteArray()

            // when
            adapter.store("teams", "logo.png", originalContent, "image/png")
            adapter.store("teams", "logo.png", newContent, "image/png")

            // then
            val storedFile = tempDir.resolve("teams").resolve("logo.png")
            assertThat(Files.readAllBytes(storedFile)).isEqualTo(newContent)
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {
        @Test
        @DisplayName("존재하는 이미지를 삭제한다")
        fun deleteExisting() {
            // given
            val content = "data".toByteArray()
            adapter.store("teams", "delete-me.png", content, "image/png")
            val filePath = tempDir.resolve("teams").resolve("delete-me.png")
            assertThat(Files.exists(filePath)).isTrue()

            // when
            adapter.delete("/images/teams/delete-me.png")

            // then
            assertThat(Files.exists(filePath)).isFalse()
        }

        @Test
        @DisplayName("존재하지 않는 이미지 삭제는 예외를 발생시키지 않는다")
        fun deleteNonExisting() {
            // when & then (no exception)
            adapter.delete("/images/teams/nonexistent.png")
        }
    }
}
