package com.nextup.core.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("BaseTimeEntity 테스트")
class BaseTimeEntityTest {
    private class TestEntity : BaseTimeEntity()

    @Nested
    @DisplayName("onPrePersist")
    inner class OnPrePersist {
        @Test
        fun `onPrePersist 호출 시 createdAt이 설정된다`() {
            val entity = TestEntity()
            val before = Instant.now()

            entity.onPrePersist()

            val after = Instant.now()
            assertThat(entity.createdAt).isAfterOrEqualTo(before)
            assertThat(entity.createdAt).isBeforeOrEqualTo(after)
        }

        @Test
        fun `onPrePersist 호출 시 updatedAt이 설정된다`() {
            val entity = TestEntity()
            val before = Instant.now()

            entity.onPrePersist()

            val after = Instant.now()
            assertThat(entity.updatedAt).isAfterOrEqualTo(before)
            assertThat(entity.updatedAt).isBeforeOrEqualTo(after)
        }

        @Test
        fun `onPrePersist 호출 시 createdAt과 updatedAt이 동일한 시각으로 설정된다`() {
            val entity = TestEntity()

            entity.onPrePersist()

            assertThat(entity.createdAt).isEqualTo(entity.updatedAt)
        }
    }

    @Nested
    @DisplayName("onPreUpdate")
    inner class OnPreUpdate {
        @Test
        fun `onPreUpdate 호출 시 updatedAt이 갱신된다`() {
            val entity = TestEntity()
            entity.onPrePersist()

            val persistedUpdatedAt = entity.updatedAt
            Thread.sleep(10)
            entity.onPreUpdate()

            assertThat(entity.updatedAt).isAfter(persistedUpdatedAt)
        }

        @Test
        fun `onPreUpdate 호출 후 createdAt은 변경되지 않는다`() {
            val entity = TestEntity()
            entity.onPrePersist()

            val persistedCreatedAt = entity.createdAt
            Thread.sleep(10)
            entity.onPreUpdate()

            assertThat(entity.createdAt).isEqualTo(persistedCreatedAt)
        }
    }
}
