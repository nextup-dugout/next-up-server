package com.nextup.infrastructure.adapter.output.persistence

import com.nextup.core.domain.game.PitchEvent
import com.nextup.core.port.repository.PitchEventRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PitchEventJpaRepository : JpaRepository<PitchEvent, Long> {
    fun findByGameIdOrderByPitchNumberAsc(gameId: Long): List<PitchEvent>

    fun findByGameIdAndInningAndIsTopInningOrderByPitchNumberAsc(
        gameId: Long,
        inning: Int,
        isTopInning: Boolean,
    ): List<PitchEvent>

    fun findByPitcherIdOrderByCreatedAtAsc(pitcherId: Long): List<PitchEvent>

    fun findByBatterIdOrderByCreatedAtAsc(batterId: Long): List<PitchEvent>

    @Query("SELECT COALESCE(MAX(pe.pitchNumber), 0) FROM PitchEvent pe WHERE pe.game.id = :gameId")
    fun findMaxPitchNumberByGameId(
        @Param("gameId") gameId: Long,
    ): Int
}

@Repository
class PitchEventRepositoryAdapter(
    private val jpaRepository: PitchEventJpaRepository,
) : PitchEventRepositoryPort {
    override fun save(pitchEvent: PitchEvent): PitchEvent = jpaRepository.save(pitchEvent)

    override fun findByIdOrNull(id: Long): PitchEvent? = jpaRepository.findById(id).orElse(null)

    override fun findByGameId(gameId: Long): List<PitchEvent> = jpaRepository.findByGameIdOrderByPitchNumberAsc(gameId)

    override fun findByGameIdAndInning(
        gameId: Long,
        inning: Int,
        isTopInning: Boolean,
    ): List<PitchEvent> =
        jpaRepository.findByGameIdAndInningAndIsTopInningOrderByPitchNumberAsc(
            gameId,
            inning,
            isTopInning,
        )

    override fun findByPitcherId(pitcherId: Long): List<PitchEvent> =
        jpaRepository.findByPitcherIdOrderByCreatedAtAsc(pitcherId)

    override fun findByBatterId(batterId: Long): List<PitchEvent> =
        jpaRepository.findByBatterIdOrderByCreatedAtAsc(batterId)

    override fun getNextPitchNumber(gameId: Long): Int = jpaRepository.findMaxPitchNumberByGameId(gameId) + 1
}
