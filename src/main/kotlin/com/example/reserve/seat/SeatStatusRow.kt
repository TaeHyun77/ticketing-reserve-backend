package com.example.reserve.seat

import com.example.reserve.seat.dto.SeatResponse
import java.time.LocalDateTime

data class SeatStatusRow(
    val seatNumber: String,
    val status: SeatStatus,
    val heldUntil: LocalDateTime?,
) {
    // Seat/isSellable 과 동일 규칙: FREE 또는 만료된 HELD 는 판매가능
    fun isReserved(now: LocalDateTime): Boolean =
        !(status == SeatStatus.FREE || (status == SeatStatus.HELD && heldUntil != null && heldUntil.isBefore(now)))

    fun toResponse(now: LocalDateTime): SeatResponse = SeatResponse(seatNumber, isReserved(now))
}
