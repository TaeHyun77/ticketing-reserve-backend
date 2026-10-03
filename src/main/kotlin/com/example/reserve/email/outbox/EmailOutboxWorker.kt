package com.example.reserve.email.outbox

import com.example.reserve.config.Loggable
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// 아웃박스 발송
// 발송 루프를 분리하여 SMTP 구간이 트랜잭션 밖에서 돌게 함
@Component
@ConditionalOnProperty(
    name = ["email.outbox.scheduler.enabled"],
    havingValue = "true",
    matchIfMissing = true, // 값이 아예 없어도 생성
)
class EmailOutboxWorker(
    private val emailOutboxService: EmailOutboxService,
) : Loggable {

    @Scheduled(fixedDelayString = "\${email.outbox.poll-interval-ms:1000}")
    fun poll() {
        val claimed = emailOutboxService.claim() // 조건에 맞는 outbox 행 배치로 n개 가져옴
        if (claimed.isEmpty()) return

        val sentIds = mutableListOf<Long>()
        val failedIds = mutableListOf<Long>()

        for (email in claimed) {
            try {
                emailOutboxService.send(email.payload) // SMTP로 이메일 발송
                sentIds += email.id
            } catch (e: Exception) {
                failedIds += email.id
                log.warn(e) { "이메일 발송 실패 - outboxId: ${email.id}" }
            }
        }
        emailOutboxService.complete(sentIds, failedIds)
    }
}
