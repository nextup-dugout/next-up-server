package com.nextup.core.domain.admin

/**
 * 조직 유형을 나타내는 열거형
 *
 * 다형적 관계(Polymorphic Association)에서 조직의 종류를 구분합니다.
 */
enum class OrganizationType(
    val displayName: String,
    val description: String,
) {
    /**
     * 협회 (최상위 조직)
     */
    ASSOCIATION("협회", "사회인 야구 협회"),

    /**
     * 리그 (협회 하위 조직)
     */
    LEAGUE("리그", "협회에 속한 리그"),

    /**
     * 팀 (리그 하위 조직)
     */
    TEAM("팀", "리그에 속한 팀"),
    ;

    companion object {
        /**
         * 문자열로부터 OrganizationType을 찾습니다.
         *
         * @param value 조직 유형 문자열
         * @return OrganizationType
         * @throws IllegalArgumentException 유효하지 않은 값인 경우
         */
        fun fromValue(value: String): OrganizationType =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid organization type: $value")
    }
}
