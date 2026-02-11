package com.nextup.core.service.user

import com.nextup.common.exception.*
import com.nextup.core.domain.user.OAuthProvider
import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.OAuthAccountRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepositoryPort,
    private val oauthAccountRepository: OAuthAccountRepositoryPort,
) {
    // ========== CREATE ==========

    /**
     * 일반 회원가입으로 사용자를 생성합니다.
     */
    @Transactional
    fun createLocalUser(
        email: String,
        encodedPassword: String,
        nickname: String,
    ): User {
        validateEmailNotDuplicate(email)

        val user =
            User.createLocalUser(
                email = email,
                encodedPassword = encodedPassword,
                nickname = nickname,
            )

        return userRepository.save(user)
    }

    /**
     * OAuth로 사용자를 생성하거나, 기존 사용자에 OAuth를 연동합니다.
     *
     * @return Pair<User, Boolean> - (사용자, 신규생성여부)
     */
    @Transactional
    fun createOrLinkOAuthUser(
        email: String,
        nickname: String,
        provider: OAuthProvider,
        oauthId: String,
        profileImageUrl: String? = null,
    ): Pair<User, Boolean> {
        // 이미 연동된 OAuth 계정이 있는지 확인
        val existingOAuth = oauthAccountRepository.findByProviderAndOauthId(provider, oauthId)
        if (existingOAuth != null) {
            return Pair(existingOAuth.user, false)
        }

        // 같은 이메일의 기존 사용자가 있는지 확인
        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) {
            // 기존 사용자에 OAuth 계정 연동
            existingUser.addOAuthAccount(provider, oauthId, email)
            if (profileImageUrl != null && existingUser.profileImageUrl == null) {
                existingUser.updateProfile(profileImageUrl = profileImageUrl)
            }
            return Pair(existingUser, false)
        }

        // 신규 사용자 생성
        val newUser =
            User.createOAuthUser(
                email = email,
                nickname = nickname,
                provider = provider,
                oauthId = oauthId,
                profileImageUrl = profileImageUrl,
            )

        return Pair(userRepository.save(newUser), true)
    }

    // ========== READ ==========

    /**
     * ID로 사용자를 조회합니다.
     */
    fun getById(id: Long): User =
        userRepository.findByIdOrNull(id)
            ?: throw UserNotFoundException(id)

    /**
     * 활성 사용자를 ID로 조회합니다.
     */
    fun getActiveById(id: Long): User {
        val user = getById(id)
        if (!user.isActive) {
            throw UserDeactivatedException(id)
        }
        return user
    }

    /**
     * 이메일로 사용자를 조회합니다.
     */
    fun getByEmail(email: String): User =
        userRepository.findByEmail(email)
            ?: throw UserNotFoundByEmailException(email)

    /**
     * 이메일로 사용자를 조회합니다 (없으면 null).
     */
    fun findByEmail(email: String): User? = userRepository.findByEmail(email)

    /**
     * OAuth 정보로 사용자를 조회합니다.
     */
    fun findByOAuth(
        provider: OAuthProvider,
        oauthId: String,
    ): User? = oauthAccountRepository.findByProviderAndOauthId(provider, oauthId)?.user

    /**
     * 활성화된 모든 사용자를 페이징으로 조회합니다.
     */
    fun getAllActive(pageable: Pageable): Page<User> = userRepository.findAllActive(pageable)

    /**
     * 모든 사용자를 페이징으로 조회합니다 (관리자용).
     */
    fun getAll(pageable: Pageable): Page<User> = userRepository.findAllByIsActive(true, pageable)

    /**
     * 활성 상태별로 사용자를 조회합니다 (관리자용).
     */
    fun getAllByStatus(
        isActive: Boolean,
        pageable: Pageable,
    ): Page<User> = userRepository.findAllByIsActive(isActive, pageable)

    /**
     * 키워드로 사용자를 검색합니다 (관리자용).
     */
    fun search(
        keyword: String,
        pageable: Pageable,
    ): Page<User> = userRepository.searchByKeyword(keyword, pageable)

    /**
     * 역할별로 사용자를 조회합니다 (관리자용).
     */
    fun getAllByRole(
        role: Role,
        pageable: Pageable,
    ): Page<User> = userRepository.findAllByRole(role, pageable)

    // ========== UPDATE ==========

    /**
     * 프로필 정보를 업데이트합니다.
     */
    @Transactional
    fun updateProfile(
        userId: Long,
        nickname: String? = null,
        profileImageUrl: String? = null,
    ): User {
        val user = getActiveById(userId)

        user.updateProfile(
            nickname = nickname ?: user.nickname,
            profileImageUrl = profileImageUrl,
        )

        return user
    }

    /**
     * 비밀번호를 변경합니다.
     */
    @Transactional
    fun changePassword(
        userId: Long,
        encodedPassword: String,
    ): User {
        val user = getActiveById(userId)
        user.changePassword(encodedPassword)
        return user
    }

    /**
     * 역할을 추가합니다 (관리자용).
     */
    @Transactional
    fun addRole(
        userId: Long,
        role: Role,
    ): User {
        val user = getById(userId)
        user.addRole(role)
        return user
    }

    /**
     * 역할을 제거합니다 (관리자용).
     */
    @Transactional
    fun removeRole(
        userId: Long,
        role: Role,
    ): User {
        val user = getById(userId)
        user.removeRole(role)
        return user
    }

    // ========== OAuth 관련 ==========

    /**
     * 기존 사용자에 OAuth 계정을 연동합니다.
     */
    @Transactional
    fun linkOAuthAccount(
        userId: Long,
        provider: OAuthProvider,
        oauthId: String,
        email: String? = null,
    ): User {
        val user = getActiveById(userId)

        // 이미 다른 사용자에게 연동된 OAuth인지 확인
        val existingOAuth = oauthAccountRepository.findByProviderAndOauthId(provider, oauthId)
        if (existingOAuth != null && existingOAuth.user.id != userId) {
            throw OAuthAccountAlreadyLinkedException(provider.displayName)
        }

        user.addOAuthAccount(provider, oauthId, email)
        return user
    }

    /**
     * OAuth 계정 연동을 해제합니다.
     */
    @Transactional
    fun unlinkOAuthAccount(
        userId: Long,
        provider: OAuthProvider,
    ): User {
        val user = getActiveById(userId)

        if (!user.canRemoveAuthMethod()) {
            throw InsufficientAuthMethodException()
        }

        user.removeOAuthAccount(provider)
        return user
    }

    /**
     * 사용자의 연동된 OAuth Provider 목록을 조회합니다.
     */
    fun getLinkedProviders(userId: Long): List<OAuthProvider> = oauthAccountRepository.findProvidersByUserId(userId)

    // ========== 상태 변경 ==========

    /**
     * 사용자를 비활성화합니다 (관리자용 또는 회원탈퇴).
     */
    @Transactional
    fun deactivate(userId: Long): User {
        val user = getById(userId)
        user.deactivate()
        return user
    }

    /**
     * 사용자를 활성화합니다 (관리자용).
     */
    @Transactional
    fun activate(userId: Long): User {
        val user = getById(userId)
        user.activate()
        return user
    }

    // ========== 유틸리티 ==========

    /**
     * 이메일 중복을 검사합니다.
     */
    fun validateEmailNotDuplicate(email: String) {
        if (userRepository.existsByEmail(email)) {
            throw EmailDuplicateException(email)
        }
    }

    /**
     * 이메일 존재 여부를 확인합니다.
     */
    fun existsByEmail(email: String): Boolean = userRepository.existsByEmail(email)
}
