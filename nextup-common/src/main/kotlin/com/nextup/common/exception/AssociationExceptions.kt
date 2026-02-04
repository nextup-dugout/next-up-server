package com.nextup.common.exception

/**
 * 협회를 찾을 수 없을 때 발생하는 예외
 */
class AssociationNotFoundException(
    associationId: Long,
) : NotFoundException(
        "ASSOCIATION_NOT_FOUND",
        "Association not found: $associationId",
    )

/**
 * 협회 이름이 중복될 때 발생하는 예외
 */
class AssociationNameDuplicateException(
    name: String,
) : BusinessException(
        "ASSOCIATION_NAME_DUPLICATE",
        "Association name already exists: $name",
    )
