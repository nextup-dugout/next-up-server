package com.nextup.core.port.service

/**
 * 이미지 저장소 포트 인터페이스
 *
 * Infrastructure 계층에서 실제 저장소(로컬 파일시스템, S3 등)를 사용하여 구현합니다.
 */
interface ImageStoragePort {
    /**
     * 이미지를 저장하고 접근 가능한 URL을 반환합니다.
     *
     * @param directory 저장 디렉토리 (예: "teams", "players", "leagues", "associations")
     * @param fileName 저장할 파일명 (확장자 포함)
     * @param content 파일 바이트 배열
     * @param contentType MIME 타입 (예: "image/png")
     * @return 저장된 이미지의 접근 URL
     */
    fun store(
        directory: String,
        fileName: String,
        content: ByteArray,
        contentType: String,
    ): String

    /**
     * 저장된 이미지를 삭제합니다.
     *
     * @param imageUrl 삭제할 이미지 URL
     */
    fun delete(imageUrl: String)
}
