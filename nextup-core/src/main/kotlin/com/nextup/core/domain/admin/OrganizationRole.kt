package com.nextup.core.domain.admin

/**
 * 조직 내 관리자 역할을 나타내는 열거형
 *
 * 조직별로 부여할 수 있는 권한 수준을 정의합니다.
 */
enum class OrganizationRole(
    val displayName: String,
    val description: String,
    val level: Int
) {
    /**
     * 관리자 - 조직의 모든 권한을 가진 최고 관리자
     */
    ADMIN("관리자", "조직의 모든 권한을 가진 최고 관리자", 100),

    /**
     * 매니저 - 조직 운영 관련 권한을 가진 관리자
     */
    MANAGER("매니저", "조직 운영 관련 권한을 가진 관리자", 50),

    /**
     * 기록원 - 경기 기록 입력 권한만 가진 관리자
     */
    SCORER("기록원", "경기 기록 입력 권한만 가진 관리자", 10);

    /**
     * 다른 역할보다 높은 권한인지 확인합니다.
     */
    fun isHigherThan(other: OrganizationRole): Boolean = this.level > other.level

    /**
     * 다른 역할보다 높거나 같은 권한인지 확인합니다.
     */
    fun isHigherOrEqual(other: OrganizationRole): Boolean = this.level >= other.level

    companion object {
        /**
         * 문자열로부터 OrganizationRole을 찾습니다.
         *
         * @param value 역할 문자열
         * @return OrganizationRole
         * @throws IllegalArgumentException 유효하지 않은 값인 경우
         */
        fun fromValue(value: String): OrganizationRole {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid organization role: $value")
        }
    }
}
