package com.nextup.infrastructure.security.userdetails

import com.nextup.core.domain.user.Role
import com.nextup.core.domain.user.User
import com.nextup.infrastructure.repository.UserJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.*

@DisplayName("CustomUserDetailsService 테스트")
class CustomUserDetailsServiceTest {
    private lateinit var userJpaRepository: UserJpaRepository
    private lateinit var service: CustomUserDetailsService

    @BeforeEach
    fun setUp() {
        userJpaRepository = mockk()
        service = CustomUserDetailsService(userJpaRepository)
    }

    private fun createTestUser(
        email: String = "test@example.com",
        password: String = "encoded_password",
        nickname: String = "테스터",
        id: Long = 1L,
        isActive: Boolean = true,
    ): User {
        val user =
            User.createLocalUser(
                email = email,
                encodedPassword = password,
                nickname = nickname,
            )
        // Set ID using reflection
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)

        if (!isActive) {
            user.deactivate()
        }

        return user
    }

    @Nested
    @DisplayName("이메일로 사용자 조회")
    inner class LoadUserByUsername {
        @Test
        fun `should return user details when user exists`() {
            // given
            val user = createTestUser()
            every { userJpaRepository.findByEmail("test@example.com") } returns user

            // when
            val result = service.loadUserByUsername("test@example.com")

            // then
            assertThat(result.username).isEqualTo("test@example.com")
            assertThat(result.password).isEqualTo("encoded_password")
            assertThat(result.isEnabled).isTrue()
        }

        @Test
        fun `should throw UsernameNotFoundException when user not found`() {
            // given
            every { userJpaRepository.findByEmail("notfound@example.com") } returns null

            // when & then
            val exception =
                assertThrows<UsernameNotFoundException> {
                    service.loadUserByUsername("notfound@example.com")
                }
            assertThat(exception.message).contains("notfound@example.com")
        }

        @Test
        fun `should return inactive user with locked account`() {
            // given
            val user = createTestUser(isActive = false)
            every { userJpaRepository.findByEmail("test@example.com") } returns user

            // when
            val result = service.loadUserByUsername("test@example.com")

            // then
            assertThat(result.isAccountNonLocked).isFalse()
            assertThat(result.isEnabled).isFalse()
        }
    }

    @Nested
    @DisplayName("ID로 사용자 조회")
    inner class LoadUserById {
        @Test
        fun `should return user details when user exists`() {
            // given
            val user = createTestUser(id = 42L)
            every { userJpaRepository.findByIdOrNull(42L) } returns user

            // when
            val result = service.loadUserById(42L)

            // then
            assertThat(result.id).isEqualTo(42L)
            assertThat(result.email).isEqualTo("test@example.com")
            assertThat(result.nickname).isEqualTo("테스터")
        }

        @Test
        fun `should throw UsernameNotFoundException when user not found`() {
            // given
            every { userJpaRepository.findByIdOrNull(999L) } returns null

            // when & then
            val exception =
                assertThrows<UsernameNotFoundException> {
                    service.loadUserById(999L)
                }
            assertThat(exception.message).contains("999")
        }
    }
}

@DisplayName("CustomUserDetails 테스트")
class CustomUserDetailsTest {
    private fun createTestUser(
        email: String = "test@example.com",
        password: String = "encoded_password",
        nickname: String = "테스터",
        id: Long = 1L,
        isActive: Boolean = true,
    ): User {
        val user =
            User.createLocalUser(
                email = email,
                encodedPassword = password,
                nickname = nickname,
            )
        // Set ID using reflection
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)

        if (!isActive) {
            user.deactivate()
        }

        return user
    }

    @Nested
    @DisplayName("UserDetails 인터페이스 구현")
    inner class UserDetailsInterface {
        @Test
        fun `should return correct username as email`() {
            // given
            val user = createTestUser(email = "user@test.com")

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            assertThat(userDetails.username).isEqualTo("user@test.com")
        }

        @Test
        fun `should return password`() {
            // given
            val user = createTestUser(password = "hashed_pwd")

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            assertThat(userDetails.password).isEqualTo("hashed_pwd")
        }

        @Test
        fun `should return authorities with ROLE_ prefix`() {
            // given
            val user = createTestUser()
            user.addRole(Role.ADMIN)

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            val authorities = userDetails.authorities.map { it.authority }
            assertThat(authorities).contains("ROLE_USER", "ROLE_ADMIN")
        }

        @Test
        fun `isAccountNonExpired should always return true`() {
            // given
            val user = createTestUser()

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            assertThat(userDetails.isAccountNonExpired).isTrue()
        }

        @Test
        fun `isCredentialsNonExpired should always return true`() {
            // given
            val user = createTestUser()

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            assertThat(userDetails.isCredentialsNonExpired).isTrue()
        }

        @Test
        fun `isAccountNonLocked should reflect user active status`() {
            // given
            val activeUser = createTestUser(isActive = true)
            val inactiveUser = createTestUser(isActive = false)

            // when
            val activeUserDetails = CustomUserDetails.from(activeUser)
            val inactiveUserDetails = CustomUserDetails.from(inactiveUser)

            // then
            assertThat(activeUserDetails.isAccountNonLocked).isTrue()
            assertThat(inactiveUserDetails.isAccountNonLocked).isFalse()
        }

        @Test
        fun `isEnabled should reflect user active status`() {
            // given
            val activeUser = createTestUser(isActive = true)
            val inactiveUser = createTestUser(isActive = false)

            // when
            val activeUserDetails = CustomUserDetails.from(activeUser)
            val inactiveUserDetails = CustomUserDetails.from(inactiveUser)

            // then
            assertThat(activeUserDetails.isEnabled).isTrue()
            assertThat(inactiveUserDetails.isEnabled).isFalse()
        }
    }

    @Nested
    @DisplayName("추가 속성")
    inner class AdditionalProperties {
        @Test
        fun `should expose user id`() {
            // given
            val user = createTestUser(id = 123L)

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            assertThat(userDetails.id).isEqualTo(123L)
        }

        @Test
        fun `should expose user email`() {
            // given
            val user = createTestUser(email = "custom@email.com")

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            assertThat(userDetails.email).isEqualTo("custom@email.com")
        }

        @Test
        fun `should expose user nickname`() {
            // given
            val user = createTestUser(nickname = "닉네임123")

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            assertThat(userDetails.nickname).isEqualTo("닉네임123")
        }

        @Test
        fun `should expose user roles as Set`() {
            // given
            val user = createTestUser()
            user.addRole(Role.SCORER)

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            assertThat(userDetails.roles).containsExactlyInAnyOrder(Role.USER, Role.SCORER)
        }

        @Test
        fun `getRoleNames should return role names without ROLE_ prefix`() {
            // given
            val user = createTestUser()
            user.addRole(Role.ADMIN)

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            val roleNames = userDetails.getRoleNames()
            assertThat(roleNames).containsExactlyInAnyOrder("USER", "ADMIN")
        }
    }

    @Nested
    @DisplayName("팩토리 메소드")
    inner class FactoryMethod {
        @Test
        fun `should create CustomUserDetails from User`() {
            // given
            val user = createTestUser()

            // when
            val userDetails = CustomUserDetails.from(user)

            // then
            assertThat(userDetails).isNotNull
            assertThat(userDetails).isInstanceOf(CustomUserDetails::class.java)
        }
    }
}
