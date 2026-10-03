package com.example.reserve.seat.dto

import com.example.reserve.seat.Seat
import java.time.LocalDateTime

// 좌석별 중복 정보를 제거해 좌석번호와 선택 가능 여부만 반환하여 페이로드 및 직렬화 비용을 절감
data class SeatResponse(
    val seatNumber: String,

    // 선택 불가 여부 ( 확정 예약 또는 유효한 홀드 = 판매 불가 )
    val isReserved: Boolean,
) {
    companion object {
        fun from(seat: Seat, now: LocalDateTime): SeatResponse =
            SeatResponse(seat.seatNumber, !seat.isSellable(now))
    }
}
