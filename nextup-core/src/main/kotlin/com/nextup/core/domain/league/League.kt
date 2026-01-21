package com.nextup.core.domain.league

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import jakarta.persistence.*

@Entity
@Table(name = "leagues")
class League(
    @Column(nullable = false, length = 100)
    val name: String,

    @Column(length = 20)
    val abbreviation: String? = null,

    @Column(length = 50)
    val country: String? = null,

    @Column(length = 50)
    val sport: String = "baseball",

    @Column(nullable = false)
    val foundedYear: Int,

    @Column(length = 500)
    var description: String? = null,

    @Column(length = 255)
    var logoUrl: String? = null,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : BaseTimeEntity() {

    @OneToMany(mappedBy = "league", fetch = FetchType.LAZY)
    private val _teams: MutableList<Team> = mutableListOf()
    val teams: List<Team> get() = _teams.toList()

    fun addTeam(team: Team) {
        _teams.add(team)
    }

    fun removeTeam(team: Team) {
        _teams.remove(team)
    }
}
