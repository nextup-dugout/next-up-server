package com.nextup.core.service.game

/**
 * 기록원 전용 경기 기록 서비스 통합 인터페이스
 *
 * 5개의 세부 서비스를 통합하는 마커 인터페이스입니다.
 * 개별 서비스를 직접 주입하는 것을 권장합니다.
 *
 * @see GameLifecycleService
 * @see PlateAppearanceRecordService
 * @see GameUndoService
 * @see BaseRunningRecordService
 * @see GameSubstitutionService
 */
interface GameScorerService :
    GameLifecycleService,
    PlateAppearanceRecordService,
    GameUndoService,
    BaseRunningRecordService,
    GameSubstitutionService
