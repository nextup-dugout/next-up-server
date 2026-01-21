package com.nextup.core.domain.team

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.PlayerTeamHistory
import jakarta.persistence.*

@Entity
@Table(
    name = "teams",
    indexes = [
        Index(name = "idx_teams_league_id", columnList = "league_id")
    ]
)
class Team(
    @Column(nullable = false, length = 100)
    val name: String,

    @Column(length = 20)
    val abbreviation: String? = null,

    @Column(nullable = false, length = 100)
    val city: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    val league: League,

    @Column(nullable = false)
    val foundedYear: Int,

    @Column(length = 255)
    var logoUrl: String? = null,

    @Column(length = 50)
    var primaryColor: String? = null,

    @Column(length = 50)
    var secondaryColor: String? = null,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : BaseTimeEntity() {

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private val _playerHistories: MutableList<PlayerTeamHistory> = mutableListOf()
    val playerHistories: List<PlayerTeamHistory> get() = _playerHistories.toList()

    val fullName: String
        get() = "$city $name"

    init {
        league.addTeam(this)
    }
}
