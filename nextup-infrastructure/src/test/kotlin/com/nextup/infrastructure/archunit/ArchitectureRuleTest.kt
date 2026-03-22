package com.nextup.infrastructure.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Path
import java.nio.file.Paths

/**
 * ArchUnit 빌드 타임 아키텍처 규칙 테스트
 *
 * 프로젝트 헌법(CLAUDE.md)에 정의된 의존성 방향 규칙과
 * 보안 규칙(#432)을 빌드 타임에 강제합니다.
 *
 * 위반 시 빌드가 실패하여 코드 리뷰 이전에 문제를 감지합니다.
 *
 * 모듈별 클래스 파일을 직접 임포트하여 의존성 검사를 수행합니다.
 * 빌드 순서에 따라 아직 컴파일되지 않은 모듈은 건너뜁니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("ArchUnit 아키텍처 규칙")
class ArchitectureRuleTest {

    private lateinit var coreClasses: JavaClasses
    private lateinit var commonClasses: JavaClasses
    private lateinit var infraClasses: JavaClasses
    private lateinit var allClasses: JavaClasses

    @BeforeAll
    fun importClasses() {
        val importer = ClassFileImporter().withImportOption(ImportOption.DoNotIncludeTests())

        // 프로젝트 루트 경로 탐색 (상대 경로를 사용하여 빌드 출력 디렉토리에서 클래스 임포트)
        val projectRoot = findProjectRoot()

        val classPaths = mutableListOf<Path>()

        val moduleDirs =
            listOf(
                "nextup-common",
                "nextup-core",
                "nextup-infrastructure",
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

        allClasses =
            if (classPaths.isNotEmpty()) {
                importer.importPaths(classPaths)
            } else {
                importer.importPackages("com.nextup")
            }

        coreClasses = importer.importPackages("com.nextup.core")
        commonClasses = importer.importPackages("com.nextup.common")
        infraClasses = importer.importPackages("com.nextup.infrastructure")
    }

    private fun findProjectRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (current.parent != null) {
            if (current.resolve("settings.gradle.kts").toFile().exists()) {
                return current
            }
            current = current.parent
        }
        // fallback: 현재 디렉토리
        return Paths.get("").toAbsolutePath()
    }

    @Nested
    @DisplayName("의존성 방향 규칙")
    inner class DependencyDirection {

        @Test
        @DisplayName("Core 모듈은 Infrastructure를 의존하지 않는다")
        fun coreDoesNotDependOnInfrastructure() {
            val rule: ArchRule =
                noClasses()
                    .that().resideInAPackage("com.nextup.core..")
                    .should().dependOnClassesThat().resideInAPackage("com.nextup.infrastructure..")

            rule.check(coreClasses)
        }

        @Test
        @DisplayName("Core 모듈은 API 계층을 의존하지 않는다")
        fun coreDoesNotDependOnApiLayer() {
            val rule: ArchRule =
                noClasses()
                    .that().resideInAPackage("com.nextup.core..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                        "com.nextup.api..",
                        "com.nextup.backoffice..",
                        "com.nextup.scorer..",
                    )

            rule.check(coreClasses)
        }

        @Test
        @DisplayName("Common 모듈은 다른 모듈을 의존하지 않는다")
        fun commonDoesNotDependOnOtherModules() {
            val rule: ArchRule =
                noClasses()
                    .that().resideInAPackage("com.nextup.common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                        "com.nextup.core..",
                        "com.nextup.infrastructure..",
                        "com.nextup.api..",
                        "com.nextup.backoffice..",
                        "com.nextup.scorer..",
                    )

            rule.check(commonClasses)
        }

        @Test
        @DisplayName("Infrastructure 모듈은 API 계층을 의존하지 않는다")
        fun infraDoesNotDependOnApiLayer() {
            val rule: ArchRule =
                noClasses()
                    .that().resideInAPackage("com.nextup.infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                        "com.nextup.api..",
                        "com.nextup.backoffice..",
                        "com.nextup.scorer..",
                    )

            rule.check(infraClasses)
        }

        @Test
        @DisplayName("Core 모듈은 Spring Data를 의존하지 않는다")
        fun coreDoesNotDependOnSpringData() {
            val rule: ArchRule =
                noClasses()
                    .that().resideInAPackage("com.nextup.core..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")

            rule.check(coreClasses)
        }
    }

    @Nested
    @DisplayName("API 계층 격리 및 보안 규칙")
    inner class CrossModuleRules {

        @Test
        @DisplayName("@RestController 클래스는 RepositoryPort를 필드로 가지지 않는다")
        fun controllersShouldNotDependOnRepositories() {
            val rule: ArchRule =
                noClasses()
                    .that().areAnnotatedWith(RestController::class.java)
                    .or().areAnnotatedWith(Controller::class.java)
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("RepositoryPort")
                    .allowEmptyShould(true)

            rule.check(allClasses)
        }

        @Test
        @DisplayName("@PostMapping 메서드는 @PreAuthorize가 있어야 한다 (클래스 또는 메서드)")
        fun postMappingRequiresPreAuthorize() {
            val rule: ArchRule =
                methods()
                    .that().areAnnotatedWith(PostMapping::class.java)
                    .and().areDeclaredInClassesThat().areAnnotatedWith(RestController::class.java)
                    .should().beAnnotatedWith(PreAuthorize::class.java)
                    .orShould().beDeclaredInClassesThat().areAnnotatedWith(PreAuthorize::class.java)
                    .allowEmptyShould(true)

            rule.check(allClasses)
        }

        @Test
        @DisplayName("@PutMapping 메서드는 @PreAuthorize가 있어야 한다 (클래스 또는 메서드)")
        fun putMappingRequiresPreAuthorize() {
            val rule: ArchRule =
                methods()
                    .that().areAnnotatedWith(PutMapping::class.java)
                    .and().areDeclaredInClassesThat().areAnnotatedWith(RestController::class.java)
                    .should().beAnnotatedWith(PreAuthorize::class.java)
                    .orShould().beDeclaredInClassesThat().areAnnotatedWith(PreAuthorize::class.java)
                    .allowEmptyShould(true)

            rule.check(allClasses)
        }

        @Test
        @DisplayName("@PatchMapping 메서드는 @PreAuthorize가 있어야 한다 (클래스 또는 메서드)")
        fun patchMappingRequiresPreAuthorize() {
            val rule: ArchRule =
                methods()
                    .that().areAnnotatedWith(PatchMapping::class.java)
                    .and().areDeclaredInClassesThat().areAnnotatedWith(RestController::class.java)
                    .should().beAnnotatedWith(PreAuthorize::class.java)
                    .orShould().beDeclaredInClassesThat().areAnnotatedWith(PreAuthorize::class.java)
                    .allowEmptyShould(true)

            rule.check(allClasses)
        }

        @Test
        @DisplayName("@DeleteMapping 메서드는 @PreAuthorize가 있어야 한다 (클래스 또는 메서드)")
        fun deleteMappingRequiresPreAuthorize() {
            val rule: ArchRule =
                methods()
                    .that().areAnnotatedWith(DeleteMapping::class.java)
                    .and().areDeclaredInClassesThat().areAnnotatedWith(RestController::class.java)
                    .should().beAnnotatedWith(PreAuthorize::class.java)
                    .orShould().beDeclaredInClassesThat().areAnnotatedWith(PreAuthorize::class.java)
                    .allowEmptyShould(true)

            rule.check(allClasses)
        }
    }
}
