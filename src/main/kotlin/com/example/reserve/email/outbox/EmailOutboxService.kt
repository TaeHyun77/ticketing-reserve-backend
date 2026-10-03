package com.example.reserve.email.outbox

import com.example.reserve.config.Loggable
import com.example.reserve.email.EmailService
import com.example.reserve.email.dto.ReservationEmailData
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// 아웃박스 적재와 발송을 담당
// 발송(SMTP)은 트랜잭션 밖에서 수행해 DB 커넥션/락을 발송 시간만큼 점유하지 않도록 세 단계로 분리
@Service
class EmailOutboxService(
    private val emailOutboxRepository: EmailOutboxRepository,
    private val emailService: EmailService,
    private val objectMapper: ObjectMapper,

    @Value("\${email.outbox.batch-size:20}") private val batchSize: Int,
    @Value("\${email.outbox.retry-delay-seconds:60}") private val retryDelaySeconds: Long,
    @Value("\${email.outbox.max-attempts:5}") private val maxAttempts: Int,
) : Loggable {

    // 반드시 기존 트랜잭션 안에서 실행되도록 ( 트랜잭션이 없다면 예외 발생, 없다고 새로운 트랜잭션 생성하지 않음 )
    @Transactional(propagation = Propagation.MANDATORY)
    fun enqueue(data: ReservationEmailData) {
        val payload = objectMapper.writeValueAsString(data)
        emailOutboxRepository.save(EmailOutbox.pending(payload, LocalDateTime.now()))
    }

    // 1단계 : 발송 대상 선점. next_attempt_at 을 미래로 밀어 다른 워커/다음 폴링이 건너뛰게 한 뒤 즉시 커밋
    // 발송 실패 시에도 이 리스가 곧 재시도 지연이 되므로, complete 에서 실패 행을 따로 손대지 않아도 됨
    @Transactional
    fun claim(): List<ClaimedEmail> {
        val leaseUntil = LocalDateTime.now().plusSeconds(retryDelaySeconds)
        return emailOutboxRepository.findDueForDispatch(LocalDateTime.now(), batchSize)
            .map { outbox ->
                outbox.retryAfter(leaseUntil) // 더티체킹으로 커밋 시 UPDATE 반영
                ClaimedEmail(outbox.id!!, outbox.payload)
            }
    }

    // 2단계 : 실제 SMTP 발송 (트랜잭션 없음 — 커넥션을 쥐지 않음)
    fun send(payload: String) {
        val data = objectMapper.readValue(payload, ReservationEmailData::class.java)
        emailService.sendReservationEmail(data)
    }

    // 3단계 : 성공 행은 삭제, 실패 행은 시도 횟수를 늘리고 상한 초과 시 dead 로 격리
    // 실패 행의 다음 시도 시각은 claim 의 리스로 이미 미뤄져 있어 여기선 손대지 않음
    @Transactional
    fun complete(sentIds: List<Long>, failedIds: List<Long>) {
        if (sentIds.isNotEmpty()) emailOutboxRepository.deleteAllById(sentIds)
        if (failedIds.isNotEmpty()) {
            emailOutboxRepository.findAllById(failedIds).forEach { it.recordFailure(maxAttempts) }
        }
    }
}
