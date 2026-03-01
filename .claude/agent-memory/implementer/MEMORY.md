# Implementer Agent Memory - NEXT-UP Project

## Key Navigation Paths (JPQL)
- GamePlayer -> game: use `gp.gameTeam.game` (NOT `gp.game` - GamePlayer has no direct game field)
- BattingRecord -> game: `br.gamePlayer.gameTeam.game`
- FieldingRecord -> game: `fr.gamePlayer.gameTeam.game`
- Game scheduled field: `game.scheduledAt` (LocalDateTime, NOT scheduledDate)

## Existing Code Patterns
- Record entities: `@ManyToOne` to GamePlayer, no `unique=true` (one per game player)
- Stats entities: player + year unique constraint for season, OneToOne for career
- Repository: Direct JPA + Port pattern (`interface Repo: JpaRepository<E,Long>, PortInterface`)
- Service: in `nextup-core/service/{domain}/` with `@Service @Transactional(readOnly=true)`
- `gamePlayerRepository.findByIdOrNull(id)` - uses `findByIdOrNull` not `findById`

## Exception Patterns
- All domain exceptions in `nextup-common/exception/`
- `RecordAlreadyExistsException(gamePlayerId, "RecordType")` for duplicates
- `StatsValidationException(message)` for stats validation
- `FieldingRecordNotFoundException(gamePlayerId)` - added in FieldingExceptions.kt

## Build Commands
- Full build: `./gradlew clean build --no-daemon --max-workers=2`
- Kill daemons: `pkill -9 -f GradleDaemon`
- Gradle always runs in background - use `sleep N && cat output_file` to check

## Module File Locations
- Core entities: `nextup-core/domain/game/` or `nextup-core/domain/stats/`
- Core ports: `nextup-core/port/repository/`
- Core services: `nextup-core/service/{game,stats}/`
- Infra repos: `nextup-infrastructure/repository/{game,stats}/`
- Migrations: `nextup-infrastructure/resources/db/migration/`
- API DTOs: `nextup-api/dto/stats/` or `nextup-api/dto/game/`
- API mappers: `nextup-api/mapper/stats/` or `nextup-api/mapper/game/`
- Scorer DTOs: `nextup-scorer/dto/{domain}/`
- Scorer controllers: `nextup-scorer/controller/{domain}/`
- Common exceptions: `nextup-common/exception/`

## Scorer URL Pattern
- Scorer uses `/api/scorer/` prefix (NOT `/api/v1/scorer/`)
- See scorer.md rules

## API URL Pattern
- API uses `/api/v1/` prefix

## ktlint Notes
- Multiline expressions must start on new line
- No trailing whitespace
- BigDecimal?.toPlainString() for nullable, .toPlainString() for non-null
