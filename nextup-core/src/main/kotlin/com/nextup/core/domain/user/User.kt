package com.nextup.core.domain.user

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import jakarta.persistence.*

/**
 * 사용자 엔티티
 *
 * 서비스 인증/인가를 담당하는 엔티티입니다.
 * Player와는 선택적 1:1 관계를 가집니다.
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_oauth", columnList = "oauth_provider, oauth_id"),
        Index(name = "idx_users_player", columnList = "player_id")
    ]
)
class User(
    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(length = 255)
    var password: String? = null,

    @Column(nullable = false, length = 50)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    val oauthProvider: OAuthProvider = OAuthProvider.LOCAL,

    @Column(name = "oauth_id", length = 100)
    val oauthId: String? = null,

    @Column(name = "profile_image_url", length = 255)
    var profileImageUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    var player: Player? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : BaseTimeEntity() {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private val _roles: MutableSet<Role> = mutableSetOf(Role.USER)

    val roles: Set<Role> get() = _roles.toSet()

    /**
     * OAuth 사용자인지 확인합니다.
     */
    val isOAuthUser: Boolean
        get() = oauthProvider != OAuthProvider.LOCAL

    /**
     * Player와 연결되어 있는지 확인합니다.
     */
    val hasLinkedPlayer: Boolean
        get() = player != null

    /**
     * 권한을 추가합니다.
     */
    fun addRole(role: Role) {
        _roles.add(role)
    }

    /**
     * 권한을 제거합니다.
     */
    fun removeRole(role: Role) {
        require(role != Role.USER) { "기본 USER 권한은 제거할 수 없습니다." }
        _roles.remove(role)
    }

    /**
     * 특정 권한을 가지고 있는지 확인합니다.
     */
    fun hasRole(role: Role): Boolean = _roles.contains(role)

    /**
     * Player 프로필을 연결합니다.
     */
    fun linkPlayer(player: Player) {
        require(this.player == null) { "이미 연결된 선수 프로필이 있습니다." }
        this.player = player
        addRole(Role.PLAYER)
    }

    /**
     * Player 프로필 연결을 해제합니다.
     */
    fun unlinkPlayer() {
        this.player = null
        removeRole(Role.PLAYER)
    }

    /**
     * 프로필 정보를 업데이트합니다.
     */
    fun updateProfile(
        nickname: String = this.nickname,
        profileImageUrl: String? = this.profileImageUrl
    ) {
        this.nickname = nickname
        this.profileImageUrl = profileImageUrl
    }

    /**
     * 비밀번호를 변경합니다 (LOCAL 사용자만).
     */
    fun changePassword(encodedPassword: String) {
        require(!isOAuthUser) { "OAuth 사용자는 비밀번호를 변경할 수 없습니다." }
        this.password = encodedPassword
    }

    /**
     * 계정을 비활성화합니다.
     */
    fun deactivate() {
        this.isActive = false
    }

    /**
     * 계정을 활성화합니다.
     */
    fun activate() {
        this.isActive = true
    }

    companion object {
        /**
         * 일반 회원가입으로 User를 생성합니다.
         */
        fun createLocalUser(
            email: String,
            encodedPassword: String,
            nickname: String
        ): User = User(
            email = email,
            password = encodedPassword,
            nickname = nickname,
            oauthProvider = OAuthProvider.LOCAL
        )

        /**
         * OAuth로 User를 생성합니다.
         */
        fun createOAuthUser(
            email: String,
            nickname: String,
            provider: OAuthProvider,
            oauthId: String,
            profileImageUrl: String? = null
        ): User = User(
            email = email,
            nickname = nickname,
            oauthProvider = provider,
            oauthId = oauthId,
            profileImageUrl = profileImageUrl
        )
    }
}
