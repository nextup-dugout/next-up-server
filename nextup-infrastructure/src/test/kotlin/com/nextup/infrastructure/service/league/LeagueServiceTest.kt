package com.nextup.infrastructure.service.league

import com.nextup.common.exception.AssociationNotFoundException
import com.nextup.common.exception.LeagueNameDuplicateException
import com.nextup.common.exception.LeagueNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.infrastructure.repository.association.AssociationRepository
import com.nextup.infrastructure.repository.league.LeagueRepository
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

@DisplayName("LeagueService")
class LeagueServiceTest {

    private lateinit var leagueRepository: LeagueRepository
    private lateinit var associationRepository: AssociationRepository
    private lateinit var leagueService: LeagueService

    @BeforeEach
    fun setUp() {
        leagueRepository = mockk()
        associationRepository = mockk()
        leagueService = LeagueService(leagueRepository, associationRepository)
    }

    @Nested
    @DisplayName("create")
    inner class Create {

        @Test
        fun `should create league successfully`() {
            // given
            val associationId = 1L
            val association = createAssociation(associationId, "서울시야구협회")
            val name = "1부 리그"
            val foundedYear = 2020

            every { associationRepository.findById(associationId) } returns Optional.of(association)
            every { leagueRepository.existsByAssociationIdAndName(associationId, name) } returns false
            every { leagueRepository.save(any()) } answers { firstArg() }

            // when
            val result = leagueService.create(
                associationId = associationId,
                name = name,
                abbreviation = "1부",
                foundedYear = foundedYear,
                divisionLevel = 1,
                description = "서울시야구협회 1부 리그"
            )

            // then
            assertThat(result.name).isEqualTo(name)
            assertThat(result.foundedYear).isEqualTo(foundedYear)
            assertThat(result.association).isEqualTo(association)
            assertThat(result.isActive).isTrue()
            verify { leagueRepository.save(any()) }
        }

        @Test
        fun `should throw exception when association not found`() {
            // given
            val associationId = 999L
            every { associationRepository.findById(associationId) } returns Optional.empty()

            // when & then
            assertThatThrownBy {
                leagueService.create(
                    associationId = associationId,
                    name = "1부 리그",
                    foundedYear = 2020
                )
            }.isInstanceOf(AssociationNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when league name is duplicated in same association`() {
            // given
            val associationId = 1L
            val association = createAssociation(associationId, "서울시야구협회")
            val name = "1부 리그"

            every { associationRepository.findById(associationId) } returns Optional.of(association)
            every { leagueRepository.existsByAssociationIdAndName(associationId, name) } returns true

            // when & then
            assertThatThrownBy {
                leagueService.create(
                    associationId = associationId,
                    name = name,
                    foundedYear = 2020
                )
            }.isInstanceOf(LeagueNameDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {

        @Test
        fun `should return league when found`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(id, "1부 리그", association)
            every { leagueRepository.findById(id) } returns Optional.of(league)

            // when
            val result = leagueService.getById(id)

            // then
            assertThat(result.id).isEqualTo(id)
            assertThat(result.name).isEqualTo("1부 리그")
        }

        @Test
        fun `should throw exception when not found`() {
            // given
            val id = 999L
            every { leagueRepository.findById(id) } returns Optional.empty()

            // when & then
            assertThatThrownBy {
                leagueService.getById(id)
            }.isInstanceOf(LeagueNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getAllActive")
    inner class GetAllActive {

        @Test
        fun `should return only active leagues`() {
            // given
            val association = createAssociation(1L, "서울시야구협회")
            val leagues = listOf(
                createLeague(1L, "1부 리그", association),
                createLeague(2L, "2부 리그", association)
            )
            every { leagueRepository.findAllActive() } returns leagues

            // when
            val result = leagueService.getAllActive()

            // then
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("getActiveByAssociationId")
    inner class GetActiveByAssociationId {

        @Test
        fun `should return active leagues by association`() {
            // given
            val associationId = 1L
            val association = createAssociation(associationId, "서울시야구협회")
            val leagues = listOf(
                createLeague(1L, "1부 리그", association),
                createLeague(2L, "2부 리그", association)
            )
            every { leagueRepository.findActiveByAssociationId(associationId) } returns leagues

            // when
            val result = leagueService.getActiveByAssociationId(associationId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.association.id == associationId }).isTrue()
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {

        @Test
        fun `should update league info`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(id, "1부 리그", association)
            every { leagueRepository.findById(id) } returns Optional.of(league)

            // when
            val result = leagueService.update(
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
        fun `should deactivate league`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(id, "1부 리그", association)
            every { leagueRepository.findById(id) } returns Optional.of(league)

            // when
            val result = leagueService.deactivate(id)

            // then
            assertThat(result.isActive).isFalse()
        }

        @Test
        fun `should activate league`() {
            // given
            val id = 1L
            val association = createAssociation(1L, "서울시야구협회")
            val league = createLeague(id, "1부 리그", association).apply { deactivate() }
            every { leagueRepository.findById(id) } returns Optional.of(league)

            // when
            val result = leagueService.activate(id)

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
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createLeague(id: Long, name: String, association: Association): League {
        return League(
            association = association,
            name = name,
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }
}
