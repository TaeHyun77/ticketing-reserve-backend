package com.example.reserve.email.outbox

// 워커가 선점한 발송 대상 한 건 - 역직렬화는 발송 시점(트랜잭션 밖)으로 미루기 위해 payload 를 원문 그대로 전달
data class ClaimedEmail(
    val id: Long,
    val payload: String,
)
