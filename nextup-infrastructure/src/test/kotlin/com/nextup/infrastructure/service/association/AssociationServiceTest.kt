package com.nextup.infrastructure.service.association

import com.nextup.common.exception.AssociationNameDuplicateException
import com.nextup.common.exception.AssociationNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.infrastructure.repository.association.AssociationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("AssociationService")
class AssociationServiceTest {

    private lateinit var associationRepository: AssociationRepository
    private lateinit var associationService: AssociationService

    @BeforeEach
    fun setUp() {
        associationRepository = mockk()
        associationService = AssociationService(associationRepository)
    }

    @Nested
    @DisplayName("create")
    inner class Create {

        @Test
        fun `should create association successfully`() {
            // given
            val name = "서울시야구협회"
            val region = "서울"
            every { associationRepository.existsByName(name) } returns false
            every { associationRepository.save(any()) } answers { firstArg() }

            // when
            val result = associationService.create(
                name = name,
                abbreviation = "서야협",
                region = region,
                description = "서울시 사회인 야구 협회"
            )

            // then
            assertThat(result.name).isEqualTo(name)
            assertThat(result.region).isEqualTo(region)
            assertThat(result.isActive).isTrue()
            verify { associationRepository.save(any()) }
        }

        @Test
        fun `should throw exception when name is duplicated`() {
            // given
            val name = "서울시야구협회"
            every { associationRepository.existsByName(name) } returns true

            // when & then
            assertThatThrownBy {
                associationService.create(name = name)
            }.isInstanceOf(AssociationNameDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {

        @Test
        fun `should return association when found`() {
            // given
            val id = 1L
            val association = createAssociation(id, "서울시야구협회")
            every { associationRepository.findById(id) } returns Optional.of(association)

            // when
            val result = associationService.getById(id)

            // then
            assertThat(result.id).isEqualTo(id)
            assertThat(result.name).isEqualTo("서울시야구협회")
        }

        @Test
        fun `should throw exception when not found`() {
            // given
            val id = 999L
            every { associationRepository.findById(id) } returns Optional.empty()

            // when & then
            assertThatThrownBy {
                associationService.getById(id)
            }.isInstanceOf(AssociationNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getAllActive")
    inner class GetAllActive {

        @Test
        fun `should return only active associations`() {
            // given
            val associations = listOf(
                createAssociation(1L, "서울시야구협회"),
                createAssociation(2L, "경기도야구협회")
            )
            every { associationRepository.findAllActive() } returns associations

            // when
            val result = associationService.getAllActive()

            // then
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {

        @Test
        fun `should update association info`() {
            // given
            val id = 1L
            val association = createAssociation(id, "서울시야구협회")
            every { associationRepository.findById(id) } returns Optional.of(association)

            // when
            val result = associationService.update(
                id = id,
                description = "새로운 설명",
                logoUrl = "https://example.com/logo.png"
            )

            // then
            assertThat(result.description).isEqualTo("새로운 설명")
            assertThat(result.logoUrl).isEqualTo("https://example.com/logo.png")
        }
    }

    @Nested
    @DisplayName("deactivate/activate")
    inner class DeactivateActivate {

        @Test
        fun `should deactivate association`() {
            // given
            val id = 1L
            val association = createAssociation(id, "서울시야구협회")
            every { associationRepository.findById(id) } returns Optional.of(association)

            // when
            val result = associationService.deactivate(id)

            // then
            assertThat(result.isActive).isFalse()
        }

        @Test
        fun `should activate association`() {
            // given
            val id = 1L
            val association = createAssociation(id, "서울시야구협회").apply { deactivate() }
            every { associationRepository.findById(id) } returns Optional.of(association)

            // when
            val result = associationService.activate(id)

            // then
            assertThat(result.isActive).isTrue()
        }
    }

    private fun createAssociation(id: Long, name: String): Association {
        return Association(
            name = name,
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null
        ).apply {
            // 테스트용으로 ID를 설정하기 위해 리플렉션 사용
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }
}
