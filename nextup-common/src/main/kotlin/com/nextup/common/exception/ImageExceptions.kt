package com.nextup.common.exception

/**
 * 이미지 업로드 시 유효하지 않은 파일 형식일 때 발생하는 예외
 */
class UnsupportedImageFormatException(
    contentType: String?,
) : InvalidInputException(
        "UNSUPPORTED_IMAGE_FORMAT",
        "지원하지 않는 이미지 형식입니다: $contentType (허용: JPEG, PNG, WebP)",
    )

/**
 * 이미지 파일 크기가 제한을 초과할 때 발생하는 예외
 */
class ImageFileSizeExceededException(
    fileSize: Long,
    maxSize: Long,
) : InvalidInputException(
        "IMAGE_FILE_SIZE_EXCEEDED",
        "이미지 파일 크기가 제한을 초과합니다: ${fileSize / 1024}KB (최대: ${maxSize / 1024}KB)",
    )

/**
 * 이미지 파일이 비어있을 때 발생하는 예외
 */
class EmptyImageFileException :
    InvalidInputException(
        "EMPTY_IMAGE_FILE",
        "이미지 파일이 비어있습니다.",
    )

/**
 * 이미지 저장에 실패했을 때 발생하는 예외
 */
class ImageStorageException(
    message: String,
) : BusinessException(
        "IMAGE_STORAGE_FAILED",
        message,
    )
