package com.nextup.core.domain.user

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import jakarta.persistence.*

/**
 * 사용자 엔티티
 *
 * 서비스 인증/인가를 담당하는 엔티티입니다.
 * Player와는 선택적 1:1 관계를 가집니다.
 * 여러 OAuth 계정을 연동할 수 있습니다 (1:N).
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_player", columnList = "player_id"),
    ],
)
class User private constructor(
    @Column(nullable = false, unique = true, length = 100)
    val email: String,
    @Column(length = 255)
    var password: String? = null,
    @Column(nullable = false, length = 50)
    var nickname: String,
    @Column(name = "profile_image_url", length = 255)
    var profileImageUrl: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    var player: Player? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private val _roles: MutableSet<Role> = mutableSetOf(Role.USER)

    @OneToMany(
        mappedBy = "user",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    private val _oauthAccounts: MutableList<OAuthAccount> = mutableListOf()

    val roles: Set<Role> get() = _roles.toSet()

    val oauthAccounts: List<OAuthAccount> get() = _oauthAccounts.toList()

    /**
     * LOCAL 사용자인지 확인합니다 (password 기반 인증).
     */
    val isLocalUser: Boolean
        get() = password != null

    /**
     * OAuth 계정이 연동되어 있는지 확인합니다.
     */
    val hasOAuthAccount: Boolean
        get() = _oauthAccounts.isNotEmpty()

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
     * OAuth 계정을 연동합니다.
     *
     * @param provider OAuth 제공자
     * @param oauthId OAuth 제공자에서 발급한 사용자 ID
     * @param email OAuth 제공자에서 제공하는 이메일 (선택)
     * @return 생성된 OAuthAccount
     * @throws IllegalArgumentException 이미 연동된 Provider인 경우
     */
    fun addOAuthAccount(
        provider: OAuthProvider,
        oauthId: String,
        email: String? = null,
    ): OAuthAccount {
        require(!hasOAuthProvider(provider)) {
            "이미 ${provider.displayName} 계정이 연결되어 있습니다."
        }
        val account = OAuthAccount.create(this, provider, oauthId, email)
        _oauthAccounts.add(account)
        return account
    }

    /**
     * OAuth 계정 연동을 해제합니다.
     *
     * @param provider 해제할 OAuth 제공자
     * @throws IllegalStateException 최소 1개의 인증 수단이 없는 경우
     */
    fun removeOAuthAccount(provider: OAuthProvider) {
        require(canRemoveAuthMethod()) {
            "최소 1개의 인증 수단이 필요합니다. 비밀번호를 설정하거나 다른 소셜 계정을 연동해주세요."
        }
        _oauthAccounts.removeIf { it.provider == provider }
    }

    /**
     * 특정 OAuth 제공자가 연동되어 있는지 확인합니다.
     */
    fun hasOAuthProvider(provider: OAuthProvider): Boolean = _oauthAccounts.any { it.provider == provider }

    /**
     * 특정 OAuth 제공자의 연동 정보를 조회합니다.
     */
    fun getOAuthAccount(provider: OAuthProvider): OAuthAccount? = _oauthAccounts.find { it.provider == provider }

    /**
     * 인증 수단을 제거할 수 있는지 확인합니다.
     * (최소 1개의 인증 수단 유지 필요)
     */
    fun canRemoveAuthMethod(): Boolean {
        val authMethodCount = (if (isLocalUser) 1 else 0) + _oauthAccounts.size
        return authMethodCount > 1
    }

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
        profileImageUrl: String? = this.profileImageUrl,
    ) {
        this.nickname = nickname
        this.profileImageUrl = profileImageUrl
    }

    /**
     * 비밀번호를 변경합니다.
     */
    fun changePassword(encodedPassword: String) {
        require(encodedPassword.isNotBlank()) { "비밀번호는 비어있을 수 없습니다." }
        this.password = encodedPassword
    }

    /**
     * 비밀번호를 제거합니다 (OAuth 전용 계정으로 전환).
     *
     * @throws IllegalStateException OAuth 계정이 없는 경우
     */
    fun removePassword() {
        require(hasOAuthAccount) {
            "비밀번호를 제거하려면 최소 1개의 소셜 계정이 연동되어 있어야 합니다."
        }
        this.password = null
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "User(id=$id, email=$email, nickname=$nickname, isActive=$isActive)"

    companion object {
        /**
         * 일반 회원가입으로 User를 생성합니다.
         */
        fun createLocalUser(
            email: String,
            encodedPassword: String,
            nickname: String,
        ): User {
            require(email.isNotBlank()) { "이메일은 비어있을 수 없습니다." }
            require(encodedPassword.isNotBlank()) { "비밀번호는 비어있을 수 없습니다." }
            require(nickname.isNotBlank()) { "닉네임은 비어있을 수 없습니다." }

            return User(
                email = email,
                password = encodedPassword,
                nickname = nickname,
            )
        }

        /**
         * OAuth로 User를 생성합니다.
         *
         * @param email 사용자 이메일
         * @param nickname 사용자 닉네임
         * @param provider OAuth 제공자
         * @param oauthId OAuth 제공자에서 발급한 사용자 ID
         * @param profileImageUrl 프로필 이미지 URL (선택)
         * @return 생성된 User (OAuthAccount가 자동으로 추가됨)
         */
        fun createOAuthUser(
            email: String,
            nickname: String,
            provider: OAuthProvider,
            oauthId: String,
            profileImageUrl: String? = null,
        ): User {
            require(email.isNotBlank()) { "이메일은 비어있을 수 없습니다." }
            require(nickname.isNotBlank()) { "닉네임은 비어있을 수 없습니다." }
            require(provider != OAuthProvider.LOCAL) { "OAuth 사용자는 LOCAL provider를 사용할 수 없습니다." }

            return User(
                email = email,
                nickname = nickname,
                profileImageUrl = profileImageUrl,
            ).apply {
                addOAuthAccount(provider, oauthId, email)
            }
        }
    }
}
