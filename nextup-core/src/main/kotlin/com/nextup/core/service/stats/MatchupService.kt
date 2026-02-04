package com.nextup.core.service.stats

import com.nextup.core.service.stats.dto.MatchupDto

interface MatchupService {
    fun getMatchup(
        pitcherId: Long,
        batterId: Long,
        year: Int?,
        competitionId: Long?,
    ): MatchupDto
}
