package com.nextup.infrastructure.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @PreAuthorize 필수 검증 ArchUnit 테스트 (#476)
 *
 * IDOR 취약점 재발 방지를 위해 CI 레벨에서 자동 감지합니다.
 *
 * 규칙:
 * - POST/PUT/PATCH/DELETE 엔드포인트에 @PreAuthorize가 없으면 빌드 실패
 * - 메서드 레벨 또는 클래스 레벨 @PreAuthorize 모두 허용
 *
 * 예외:
 * - 인증 자체를 수행하는 엔드포인트 (/auth/ 경로)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("@PreAuthorize 필수 보안 규칙 (#476)")
class PreAuthorizeSecurityTest {

    private lateinit var allClasses: JavaClasses

    companion object {
        /**
         * 인증 관련 경로 패턴 — 이 경로를 가진 컨트롤러는 @PreAuthorize 검증에서 제외
         *
         * /auth/login, /auth/refresh, /auth/logout 등 인증 자체를 수행하는 엔드포인트는
         * 인증 전 접근이 필요하므로 @PreAuthorize 강제 대상이 아닙니다.
         */
        private val AUTH_PATH_PATTERNS = listOf("/auth/", "/auth")

        /** mutating HTTP 메서드 어노테이션 목록 */
        private val MUTATING_ANNOTATIONS: List<Class<out Annotation>> =
            listOf(
                PostMapping::class.java,
                PutMapping::class.java,
                PatchMapping::class.java,
                DeleteMapping::class.java,
            )
    }

    @BeforeAll
    fun importClasses() {
        val importer = ClassFileImporter().withImportOption(ImportOption.DoNotIncludeTests())
        val projectRoot = findProjectRoot()
        val classPaths = mutableListOf<Path>()

        val moduleDirs =
            listOf(
                "nextup-api",
                "nextup-backoffice",
                "nextup-scorer",
            )

        for (module in moduleDirs) {
            val classDir = projectRoot.resolve("$module/build/classes/kotlin/main")
            if (classDir.toFile().exists()) {
                classPaths.add(classDir)
            }
        }

        // 인프라 모듈도 포함 (컨트롤러가 참조하는 클래스 해석을 위해)
        val infraClassDir = projectRoot.resolve("nextup-infrastructure/build/classes/kotlin/main")
        if (infraClassDir.toFile().exists()) {
            classPaths.add(infraClassDir)
        }

        allClasses =
            if (classPaths.isNotEmpty()) {
                importer.importPaths(classPaths)
            } else {
                importer.importPackages("com.nextup")
            }
    }

    private fun findProjectRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (current.parent != null) {
            if (current.resolve("settings.gradle.kts").toFile().exists()) {
                return current
            }
            current = current.parent
        }
        return Paths.get("").toAbsolutePath()
    }

    @Test
    @DisplayName("mutating 엔드포인트(@Post/@Put/@Patch/@Delete)는 @PreAuthorize가 필수이다 (인증 경로 제외)")
    fun mutatingEndpointsMustHavePreAuthorize() {
        val rule =
            methods()
                .that().areAnnotatedWith(PostMapping::class.java)
                .or().areAnnotatedWith(PutMapping::class.java)
                .or().areAnnotatedWith(PatchMapping::class.java)
                .or().areAnnotatedWith(DeleteMapping::class.java)
                .should(havePreAuthorizeUnlessAuthEndpoint())
                .allowEmptyShould(true)

        rule.check(allClasses)
    }

    /**
     * 커스텀 ArchCondition: @PreAuthorize 존재 여부를 검증하되, 인증 경로는 제외
     *
     * 검증 로직:
     * 1. 메서드가 @RestController 클래스에 선언되었는지 확인
     * 2. 클래스의 @RequestMapping 경로가 /auth/ 패턴과 일치하면 PASS (면제)
     * 3. 메서드 레벨 @PreAuthorize가 있으면 PASS
     * 4. 클래스 레벨 @PreAuthorize가 있으면 PASS
     * 5. 위 조건을 모두 만족하지 않으면 FAIL
     */
    private fun havePreAuthorizeUnlessAuthEndpoint(): ArchCondition<JavaMethod> =
        object : ArchCondition<JavaMethod>(
            "have @PreAuthorize (class or method level), unless declared in an auth endpoint controller",
        ) {
            override fun check(
                method: JavaMethod,
                events: ConditionEvents,
            ) {
                val ownerClass = method.owner

                // @RestController가 아닌 클래스의 메서드는 검증 대상이 아님
                if (!ownerClass.isAnnotatedWith(RestController::class.java)) {
                    // 조건을 만족하는 것으로 간주 (대상이 아니므로)
                    events.add(SimpleConditionEvent.satisfied(method, "${method.fullName} is not in a @RestController"))
                    return
                }

                // 인증 경로 면제 확인
                if (isAuthEndpoint(ownerClass)) {
                    events.add(
                        SimpleConditionEvent.satisfied(
                            method,
                            "${method.fullName} is exempt (auth endpoint: ${getRequestMappingPath(ownerClass)})",
                        ),
                    )
                    return
                }

                // @PreAuthorize 존재 여부 확인 (메서드 레벨 또는 클래스 레벨)
                val hasMethodLevel = method.isAnnotatedWith(PreAuthorize::class.java)
                val hasClassLevel = ownerClass.isAnnotatedWith(PreAuthorize::class.java)

                if (hasMethodLevel || hasClassLevel) {
                    events.add(
                        SimpleConditionEvent.satisfied(
                            method,
                            "${method.fullName} has @PreAuthorize",
                        ),
                    )
                } else {
                    val mappingType = getMutatingAnnotationName(method)
                    events.add(
                        SimpleConditionEvent.violated(
                            method,
                            "${method.fullName} is annotated with @$mappingType " +
                                "but has no @PreAuthorize (neither method nor class level). " +
                                "Controller: ${ownerClass.simpleName}. " +
                                "IDOR 취약점 방지를 위해 @PreAuthorize를 추가하세요.",
                        ),
                    )
                }
            }
        }

    /**
     * 컨트롤러의 @RequestMapping 경로가 인증 관련 경로인지 확인
     */
    private fun isAuthEndpoint(javaClass: com.tngtech.archunit.core.domain.JavaClass): Boolean {
        val path = getRequestMappingPath(javaClass)
        return AUTH_PATH_PATTERNS.any { pattern -> path.contains(pattern) }
    }

    /**
     * 클래스의 @RequestMapping value/path를 추출
     */
    private fun getRequestMappingPath(javaClass: com.tngtech.archunit.core.domain.JavaClass): String {
        if (!javaClass.isAnnotatedWith(RequestMapping::class.java)) {
            return ""
        }

        val annotation = javaClass.getAnnotationOfType(RequestMapping::class.java)
        val paths = annotation.value.ifEmpty { annotation.path }
        return paths.firstOrNull() ?: ""
    }

    /**
     * 메서드에 적용된 mutating 어노테이션 이름을 반환
     */
    private fun getMutatingAnnotationName(method: JavaMethod): String {
        for (annotation in MUTATING_ANNOTATIONS) {
            if (method.isAnnotatedWith(annotation)) {
                return annotation.simpleName
            }
        }
        return "Unknown"
    }
}
