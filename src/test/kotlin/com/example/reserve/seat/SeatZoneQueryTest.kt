package com.example.reserve.seat

import com.example.reserve.support.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * 구역 좌석맵 조회 검증
 * - zone 컬럼 일치 비교로 해당 구역 좌석만 반환
 */
class SeatZoneQueryTest : IntegrationTestSupport() {

    @Autowired private lateinit var seatService: SeatService

    @Test
    @DisplayName("구역 조회 - 요청한 구역의 좌석만 반환한다")
    fun `구역 좌석만 조회`() {
        val scheduleId = saveScheduleWithSeats(seatNumbers = listOf("A1", "A2"), zone = "F-19")
        val schedule = performanceScheduleRepository.findById(scheduleId).get()
        seatRepository.save(Seat(seatNumber = "B1", zone = "F-20", performanceSchedule = schedule))

        val seats = seatService.getSeatList(scheduleId, "F-19")

        assertThat(seats.map { it.seatNumber }).containsExactlyInAnyOrder("A1", "A2")
    }

    @Test
    @DisplayName("구역 조회 - LIKE 와일드카드 입력은 전체 좌석으로 확장되지 않는다")
    fun `와일드카드 구역 조회`() {
        val scheduleId = saveScheduleWithSeats(seatNumbers = listOf("A1", "A2"), zone = "F-19")

        assertThat(seatService.getSeatList(scheduleId, "%")).isEmpty()
    }
}
