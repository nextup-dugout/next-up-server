package com.nextup.core.service.image

/**
 * 이미지 업로드 서비스 인터페이스
 *
 * 이미지 업로드, 엔티티 로고/프로필 이미지 설정 등의 유스케이스를 정의합니다.
 * Infrastructure 계층에서 구현합니다.
 */
interface ImageUploadService {
    /**
     * 범용 이미지를 업로드합니다.
     *
     * @param directory 저장 디렉토리 카테고리
     * @param originalFileName 원본 파일명
     * @param content 파일 바이트 배열
     * @param contentType MIME 타입
     * @return 저장된 이미지 URL
     */
    fun uploadImage(
        directory: String,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String

    /**
     * 팀 로고를 업로드하고 팀에 설정합니다.
     *
     * @param teamId 팀 ID
     * @param originalFileName 원본 파일명
     * @param content 파일 바이트 배열
     * @param contentType MIME 타입
     * @return 저장된 이미지 URL
     */
    fun uploadTeamLogo(
        teamId: Long,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String

    /**
     * 선수 프로필 이미지를 업로드하고 설정합니다.
     *
     * @param userId 인증된 사용자 ID
     * @param originalFileName 원본 파일명
     * @param content 파일 바이트 배열
     * @param contentType MIME 타입
     * @return 저장된 이미지 URL
     */
    fun uploadPlayerProfileImage(
        userId: Long,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String

    /**
     * 리그 로고를 업로드하고 설정합니다.
     *
     * @param leagueId 리그 ID
     * @param originalFileName 원본 파일명
     * @param content 파일 바이트 배열
     * @param contentType MIME 타입
     * @return 저장된 이미지 URL
     */
    fun uploadLeagueLogo(
        leagueId: Long,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String

    /**
     * 협회 로고를 업로드하고 설정합니다.
     *
     * @param associationId 협회 ID
     * @param originalFileName 원본 파일명
     * @param content 파일 바이트 배열
     * @param contentType MIME 타입
     * @return 저장된 이미지 URL
     */
    fun uploadAssociationLogo(
        associationId: Long,
        originalFileName: String,
        content: ByteArray,
        contentType: String,
    ): String
}
