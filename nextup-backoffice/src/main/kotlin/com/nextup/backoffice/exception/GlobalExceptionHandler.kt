package com.nextup.backoffice.exception

import com.nextup.infrastructure.exception.BaseExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler : BaseExceptionHandler()
