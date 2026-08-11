package com.group3.vitamins.activitylog.domain;

public enum ActivityLogAction {
    CREATE,
    MODIFY,
    DELETE,
    /** 휴지통에서 복원. 현재는 File 도메인만 발행한다. */
    RESTORE,
    /** 휴지통에서 영구 삭제(하드 삭제). 현재는 File 도메인만 발행한다. */
    PURGE
}
