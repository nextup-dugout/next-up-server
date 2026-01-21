package com.nextup.core.domain.player

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "players",
    indexes = [
        Index(name = "idx_players_name", columnList = "name"),
        Index(name = "idx_players_birth_date", columnList = "birth_date")
    ]
)
class Player(
    @Column(nullable = false, length = 50)
    val name: String,

    @Column(name = "birth_date")
    val birthDate: LocalDate? = null,

    @Column(length = 50)
    val birthPlace: String? = null,

    @Column(length = 50)
    val nationality: String? = null,

    @Column
    var height: Int? = null,

    @Column
    var weight: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    var throwingHand: ThrowingHand? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    var battingHand: BattingHand? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var primaryPosition: Position,

    @Column
    var debutYear: Int? = null,

    @Column
    var retirementYear: Int? = null,

    @Column(length = 255)
    var profileImageUrl: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : BaseTimeEntity() {

    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _teamHistories: MutableList<PlayerTeamHistory> = mutableListOf()
    val teamHistories: List<PlayerTeamHistory> get() = _teamHistories.toList()

    val isActive: Boolean
        get() = retirementYear == null

    val currentTeamHistory: PlayerTeamHistory?
        get() = _teamHistories.find { it.endDate == null }

    val currentTeam: Team?
        get() = currentTeamHistory?.team

    fun joinTeam(
        team: Team,
        startDate: LocalDate,
        uniformNumber: Int? = null,
        position: Position = primaryPosition,
        contractType: ContractType = ContractType.REGULAR
    ): PlayerTeamHistory {
        currentTeamHistory?.endAffiliation(startDate.minusDays(1))

        val history = PlayerTeamHistory(
            player = this,
            team = team,
            startDate = startDate,
            uniformNumber = uniformNumber,
            position = position,
            contractType = contractType
        )
        _teamHistories.add(history)
        return history
    }

    fun leaveCurrentTeam(endDate: LocalDate) {
        currentTeamHistory?.endAffiliation(endDate)
    }

    fun retire(year: Int) {
        retirementYear = year
        currentTeamHistory?.endAffiliation(LocalDate.of(year, 12, 31))
    }
}
