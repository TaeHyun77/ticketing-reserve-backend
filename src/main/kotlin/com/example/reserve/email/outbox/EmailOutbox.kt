package com.example.reserve.email.outbox

import com.example.reserve.BaseTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "email_outbox",
    indexes = [Index(name = "idx_outbox_next_attempt", columnList = "next_attempt_at")]
)
class EmailOutbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_outbox_id")
    val id: Long? = null,

    // ReservationEmailData 를 직렬화한 JSON 스냅샷
    @Column(columnDefinition = "TEXT", nullable = false)
    val payload: String,

    @Column(nullable = false)
    var nextAttemptAt: LocalDateTime,
) : BaseTime() {

    @Column(nullable = false)
    var attemptCount: Int = 0

    // 재시도 상한 초과 시 true - 폴링 대상에서 제외되어 무한 재시도를 멈춤
    @Column(nullable = false)
    var dead: Boolean = false

    // 발송 실패 시 다음 시도 시각을 미래로 미룸
    fun retryAfter(next: LocalDateTime) {
        nextAttemptAt = next
    }

    // 발송 실패 기록 - 시도 횟수를 늘리고, 상한에 도달하면 재시도 대상에서 제외
    fun recordFailure(maxAttempts: Int) {
        attemptCount++
        if (attemptCount >= maxAttempts) dead = true
    }

    companion object {
        fun pending(payload: String, now: LocalDateTime): EmailOutbox =
            EmailOutbox(payload = payload, nextAttemptAt = now)
    }
}
