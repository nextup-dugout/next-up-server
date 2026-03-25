package com.nextup.core.domain.game

/**
 * 투수 기록 정정 도메인 서비스
 *
 * PitchingRecord의 관리자 정정 시스템을 캡슐화합니다.
 * PitchingCorrectionField sealed class를 활용하여 타입 안전한 필드 정정을 수행합니다.
 */
object PitchingRecordCorrector {
    /**
     * 관리자 기록 정정: 특정 필드 값을 직접 설정합니다.
     *
     * 경기 상태와 무관하게 관리자 권한으로 정정 가능합니다.
     * 정정 후 validate()를 호출하여 일관성을 검증합니다.
     *
     * @param record 정정 대상 PitchingRecord
     * @param fieldName 정정할 필드명
     * @param newValue 새로운 값 (문자열, 파싱하여 적용)
     * @param starterWinQualificationOuts 선발 승리 자격 최소 아웃 수 (기본 15 = 5이닝)
     * @return 정정 전 이전 값 (문자열)
     * @throws IllegalArgumentException 유효하지 않은 필드명 또는 값
     */
    fun correctField(
        record: PitchingRecord,
        fieldName: String,
        newValue: String,
        starterWinQualificationOuts: Int = 15,
    ): String {
        val correctionField = PitchingCorrectionField.fromFieldName(fieldName)
        val oldValue = correctionField.apply(record, newValue)
        record.validate(starterWinQualificationOuts)
        return oldValue
    }
}
