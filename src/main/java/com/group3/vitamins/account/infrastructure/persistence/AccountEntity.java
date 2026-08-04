package com.group3.vitamins.account.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 인증 전용 계정. 팀 ERD 의 {@code account} 테이블.
 *
 * <p>⚠️ <b>이 엔티티는 로그인 순간에만 등장한다.</b> 다른 도메인은 사람을 가리킬 때
 * {@code employee.user_id}(사번) 를 쓴다. {@code account_id} 를 외부로 흘리지 마라.
 *
 * <p>조회(로그인 화면에 필요한 사원·부서·직급 정보)는 MyBatis 로 조인 1방에 가져온다.
 * 이 엔티티는 <b>쓰기 전용</b>이다 — 실패 카운트·잠금·마지막 로그인·비밀번호 변경.
 */
@Entity
@Table(name = "account")
// 변경된 컬럼만 UPDATE 한다 — 두 관리자가 같은 계정을 동시에 건드릴 때(예: role 변경 vs 비번 재설정)
// 행 전체 갱신이 서로의 다른 컬럼을 덮어쓰는 lost-update 를 막는다. 같은 컬럼 동시 변경은 여전히 last-wins.
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    /** 사번. 로그인 아이디로도 쓴다 (`employee.user_id` 참조) */
    @Column(name = "user_id", nullable = false, length = 20, updatable = false)
    private String userId;

    /** Argon2id 해시 (컬럼 길이는 JPA 기본 255) */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * ADMIN &gt; MASTER &gt; MEMBER (서열형 단일값).
     *
     * <p><b>DB 컬럼은 {@code ENUM} 이다.</b> 권한 모델은 값이 늘어나는 성질이 아니라 닫힌 집합이고,
     * 앱을 거치지 않는 경로(수동 SQL · 파이썬 서버)에서도 쓰레기 값이 못 들어가야 한다.
     * 선언 순서가 서열(`global/PERMISSION.md`)과 같아 {@code ORDER BY role} 이 곧 권한 순이다.
     *
     * <p>⚠️ {@code columnDefinition} 이 없으면 {@code ddl-auto: validate} 가 varchar 를 기대해
     * <b>기동을 막는다</b> ({@code found [enum (Types#CHAR)], but expecting [varchar(20)]}).
     *
     * <p>🚨 DB 의 ENUM 값을 바꾸면 <b>이 문자열도 같이 바꿔야 한다.</b> 안 그러면 기동이 막힌다 —
     * 바꿔 말하면 DB 와 코드의 허용값이 어긋나는 순간 <b>기동 시점에 잡힌다.</b>
     */
    @Column(name = "role", nullable = false,
            columnDefinition = "enum('ADMIN','MASTER','MEMBER')")
    private String role;

    /** ACTIVE · INACTIVE — {@link #role} 과 같은 이유로 {@code columnDefinition} 을 명시한다 */
    @Column(name = "status", nullable = false,
            columnDefinition = "enum('ACTIVE','INACTIVE')")
    private String status;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** 계정 발급 — ADMIN 이 사원에게 아이디와 초기 비밀번호를 준다. 초기 비밀번호는 반드시 변경해야 한다 */
    public static AccountEntity issue(String userId, String encodedPassword, String role) {
        AccountEntity account = new AccountEntity();
        account.userId = userId;
        account.password = encodedPassword;
        account.role = role;
        account.status = "ACTIVE";
        account.mustChangePassword = true;
        account.loginFailCount = 0;
        return account;
    }

    // ===== 상태 판정 =====

    public boolean isInactive() {
        return !"ACTIVE".equals(status);
    }

    // getRole() · getStatus() 는 클래스 @Getter 가 생성한다 (수동 정의 제거)

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    // ===== 상태 변경 =====

    /**
     * 로그인 실패 누적. 임계치에 닿으면 잠근다.
     *
     * <p>잠금과 동시에 카운트를 0으로 되돌린다 — 그래야 잠금이 풀린 뒤 다시 5회의 기회가 생긴다.
     * 되돌리지 않으면 잠금 해제 직후 1회만 틀려도 다시 잠긴다.
     */
    public void recordLoginFailure(int threshold, LocalDateTime lockUntil) {
        this.loginFailCount++;
        if (this.loginFailCount >= threshold) {
            this.loginFailCount = 0;
            this.lockedUntil = lockUntil;
        }
    }

    public void recordLoginSuccess(LocalDateTime now) {
        this.loginFailCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = now;
    }

    /** 비밀번호 변경 — 초기 비밀번호 강제 변경 플래그도 함께 내린다 */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.mustChangePassword = false;
    }

    // ===== 관리자(ADMIN) 조작 — `.ai/api/account.md` =====

    /**
     * 전역 권한 변경. 허용값은 {@code MASTER} · {@code MEMBER} 뿐이다 —
     * {@code ADMIN} 부여 차단은 서비스단에서 코드({@code ACC_ADMIN_ROLE_NOT_ALLOWED})와 함께 막는다.
     */
    public void changeRole(String role) {
        this.role = role;
    }

    /** 계정 활성/비활성 토글 ({@code ACTIVE} · {@code INACTIVE}) */
    public void changeStatus(String status) {
        this.status = status;
    }

    /**
     * 관리자에 의한 비밀번호 재설정.
     *
     * <p>초기 비밀번호로 돌아가므로 {@code mustChangePassword} 를 다시 세운다 —
     * 사용자는 이 임시 비밀번호로 로그인한 뒤 반드시 새 비밀번호를 정해야 한다.
     *
     * <p>🔓 <b>잠금·실패 카운트도 함께 푼다.</b> 잠긴 사용자를 돕기 위한 재설정인데
     * 잠금이 남아 있으면 새 비밀번호로도 로그인하지 못해 재설정이 무의미해진다.
     */
    public void resetPassword(String encodedPassword) {
        this.password = encodedPassword;
        this.mustChangePassword = true;
        this.loginFailCount = 0;
        this.lockedUntil = null;
    }
}
