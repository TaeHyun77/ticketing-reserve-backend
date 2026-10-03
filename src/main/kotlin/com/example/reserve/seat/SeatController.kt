package com.example.reserve.seat

import com.example.reserve.config.Loggable
import com.example.reserve.seat.dto.SeatResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RequestMapping("/reserve/seat")
@RestController
class SeatController(
    private val seatService: SeatService
): Loggable {

    // 전체 좌석 조회 ( 콜드패스 )
    @GetMapping("/get/list/{performanceScheduleId}")
    fun getSeatList(
        @PathVariable("performanceScheduleId") performanceScheduleId: Long
    ): List<SeatResponse> = seatService.getSeatList(performanceScheduleId)

    // 구역 좌석 조회 ( 핫패스 - 선택한 구역의 좌석만 반환 )
    @GetMapping("/get/list/{performanceScheduleId}/{zone}")
    fun getSeatListByZone(
        @PathVariable("performanceScheduleId") performanceScheduleId: Long,
        @PathVariable("zone") zone: String,
    ): List<SeatResponse> = seatService.getSeatList(performanceScheduleId, zone)

}


