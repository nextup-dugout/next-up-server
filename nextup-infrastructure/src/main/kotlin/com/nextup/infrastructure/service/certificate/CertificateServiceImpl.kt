package com.nextup.infrastructure.service.certificate

import com.nextup.common.exception.CertificateNotFoundByIssueNumberException
import com.nextup.common.exception.CertificateNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.certificate.Certificate
import com.nextup.core.dto.certificate.*
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CertificateRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.service.QrCodeGeneratorPort
import com.nextup.core.service.certificate.CertificateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 증명서 서비스 구현
 *
 * 선수의 경기 기록을 기반으로 증명서를 발급하고 관리합니다.
 */
@Service
@Transactional(readOnly = true)
class CertificateServiceImpl(
    private val certificateRepository: CertificateRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val qrCodeGenerator: QrCodeGeneratorPort,
) : CertificateService {
    @Transactional
    override fun issueCertificate(
        playerId: Long,
        validityDays: Long,
    ): CertificateDto {
        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw PlayerNotFoundException(playerId)

        // 증명서 발급
        val certificate = Certificate.issue(player, validityDays)
        val savedCertificate = certificateRepository.save(certificate)

        // 선수 경력 정보 수집
        val playerCareer = buildPlayerCareer(playerId)

        // QR 코드 생성
        val qrCodeData = qrCodeGenerator.generate(savedCertificate.issueNumber)

        return savedCertificate.toDto(player.name, playerCareer, qrCodeData)
    }

    override fun getCertificate(certificateId: Long): CertificateDto {
        val certificate =
            certificateRepository.findById(certificateId)
                ?: throw CertificateNotFoundException(certificateId)

        val playerCareer = buildPlayerCareer(certificate.player.id)
        val qrCodeData = qrCodeGenerator.generate(certificate.issueNumber)

        return certificate.toDto(certificate.player.name, playerCareer, qrCodeData)
    }

    override fun getCertificateByIssueNumber(issueNumber: String): CertificateDto {
        val certificate =
            certificateRepository.findByIssueNumber(issueNumber)
                ?: throw CertificateNotFoundByIssueNumberException(issueNumber)

        val playerCareer = buildPlayerCareer(certificate.player.id)
        val qrCodeData = qrCodeGenerator.generate(certificate.issueNumber)

        return certificate.toDto(certificate.player.name, playerCareer, qrCodeData)
    }

    override fun verifyCertificate(issueNumber: String): CertificateVerificationDto {
        val certificate =
            certificateRepository.findByIssueNumber(issueNumber)
                ?: throw CertificateNotFoundByIssueNumberException(issueNumber)

        return CertificateVerificationDto(
            valid = certificate.isValid(),
            issueNumber = certificate.issueNumber,
            playerName = certificate.player.name,
            issuedAt = certificate.issuedAt,
            expiresAt = certificate.expiresAt,
            status = certificate.status,
        )
    }

    override fun getPlayerCertificates(playerId: Long): List<CertificateDto> {
        val certificates = certificateRepository.findAllByPlayerId(playerId)
        val playerCareer = buildPlayerCareer(playerId)

        return certificates.map { certificate ->
            val player = certificate.player
            val qrCodeData = qrCodeGenerator.generate(certificate.issueNumber)
            certificate.toDto(player.name, playerCareer, qrCodeData)
        }
    }

    @Transactional
    override fun revokeCertificate(certificateId: Long) {
        val certificate =
            certificateRepository.findById(certificateId)
                ?: throw CertificateNotFoundException(certificateId)

        certificate.revoke()
        certificateRepository.save(certificate)
    }

    /**
     * 선수의 경력 정보를 수집합니다.
     */
    private fun buildPlayerCareer(playerId: Long): PlayerCareerDto {
        // 출전한 모든 경기 조회
        val gamePlayers = gamePlayerRepository.findAllByPlayerId(playerId)
        val totalGames = gamePlayers.size

        // 타격 기록 수집
        val battingRecords = battingRecordRepository.findAllByPlayerId(playerId)
        val battingStats =
            if (battingRecords.isNotEmpty()) {
                buildCareerBattingStats(battingRecords)
            } else {
                null
            }

        // 투수 기록 수집
        val pitchingRecords = pitchingRecordRepository.findAllByPlayerId(playerId)
        val pitchingStats =
            if (pitchingRecords.isNotEmpty()) {
                buildCareerPitchingStats(pitchingRecords)
            } else {
                null
            }

        // 대회 이력 수집
        val competitionHistory = buildCompetitionHistory(gamePlayers)

        return PlayerCareerDto(
            totalGames = totalGames,
            battingStats = battingStats,
            pitchingStats = pitchingStats,
            competitionHistory = competitionHistory,
        )
    }

    /**
     * 통산 타격 기록을 계산합니다.
     */
    private fun buildCareerBattingStats(
        battingRecords: List<com.nextup.core.domain.game.BattingRecord>
    ): CareerBattingStatsDto {
        var games = 0
        var plateAppearances = 0
        var atBats = 0
        var hits = 0
        var doubles = 0
        var triples = 0
        var homeRuns = 0
        var runsBattedIn = 0
        var stolenBases = 0

        battingRecords.forEach { record ->
            games++
            plateAppearances += record.plateAppearances
            atBats += record.atBats
            hits += record.hits
            doubles += record.doubles
            triples += record.triples
            homeRuns += record.homeRuns
            runsBattedIn += record.runsBattedIn
            stolenBases += record.stolenBases
        }

        val battingAverage = if (atBats > 0) hits.toDouble() / atBats else 0.0
        val onBasePercentage = if (plateAppearances > 0) hits.toDouble() / plateAppearances else 0.0
        val sluggingPercentage =
            if (atBats > 0) {
                (hits + doubles + (triples * 2) + (homeRuns * 3)).toDouble() / atBats
            } else {
                0.0
            }

        return CareerBattingStatsDto(
            games = games,
            plateAppearances = plateAppearances,
            atBats = atBats,
            hits = hits,
            doubles = doubles,
            triples = triples,
            homeRuns = homeRuns,
            runsBattedIn = runsBattedIn,
            stolenBases = stolenBases,
            battingAverage = battingAverage,
            onBasePercentage = onBasePercentage,
            sluggingPercentage = sluggingPercentage,
        )
    }

    /**
     * 통산 투수 기록을 계산합니다.
     */
    private fun buildCareerPitchingStats(
        pitchingRecords: List<com.nextup.core.domain.game.PitchingRecord>,
    ): CareerPitchingStatsDto {
        var games = 0
        var wins = 0
        var losses = 0
        var saves = 0
        var holds = 0
        var inningsPitchedOuts = 0
        var strikeouts = 0
        var walks = 0
        var earnedRuns = 0

        pitchingRecords.forEach { record ->
            games++
            wins += if (record.decision?.name == "WIN") 1 else 0
            losses += if (record.decision?.name == "LOSS") 1 else 0
            saves += if (record.decision?.name == "SAVE") 1 else 0
            holds += if (record.decision?.name == "HOLD") 1 else 0
            inningsPitchedOuts += record.inningsPitchedOuts
            strikeouts += record.strikeouts
            walks += record.walksAllowed
            earnedRuns += record.earnedRuns
        }

        val inningsPitched = inningsPitchedOuts / 3.0
        val earnedRunAverage =
            if (inningsPitched > 0) {
                (earnedRuns * 9.0) / inningsPitched
            } else {
                0.0
            }
        val walksAndHitsPerInningPitched =
            if (inningsPitched > 0) {
                (walks + earnedRuns) / inningsPitched
            } else {
                0.0
            }

        return CareerPitchingStatsDto(
            games = games,
            wins = wins,
            losses = losses,
            saves = saves,
            holds = holds,
            inningsPitched = inningsPitched,
            strikeouts = strikeouts,
            walks = walks,
            earnedRuns = earnedRuns,
            earnedRunAverage = earnedRunAverage,
            walksAndHitsPerInningPitched = walksAndHitsPerInningPitched,
        )
    }

    /**
     * 대회 이력을 수집합니다.
     */
    private fun buildCompetitionHistory(
        gamePlayers: List<com.nextup.core.domain.game.GamePlayer>,
    ): List<CompetitionHistoryDto> {
        return gamePlayers
            .groupBy { gamePlayer ->
                val game = gamePlayer.gameTeam.game
                val year = game.scheduledAt.year
                val competition = game.competition
                val team = gamePlayer.gameTeam.team
                Triple(year, competition?.name ?: "연습 경기", team.name)
            }
            .map { (key, players) ->
                val (year, competitionName, teamName) = key
                CompetitionHistoryDto(
                    year = year,
                    competitionName = competitionName,
                    teamName = teamName,
                    games = players.size,
                )
            }
            .sortedByDescending { it.year }
    }

    /**
     * Certificate를 DTO로 변환합니다.
     */
    private fun Certificate.toDto(
        playerName: String,
        playerCareer: PlayerCareerDto,
        qrCodeData: String,
    ): CertificateDto =
        CertificateDto(
            id = this.id,
            issueNumber = this.issueNumber,
            playerId = this.player.id,
            playerName = playerName,
            issuedAt = this.issuedAt,
            expiresAt = this.expiresAt,
            status = this.status,
            playerCareer = playerCareer,
            qrCodeData = qrCodeData,
        )
}
