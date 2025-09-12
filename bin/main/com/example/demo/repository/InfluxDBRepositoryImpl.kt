package com.example.demo.repository

import com.example.demo.domain.SensorMeasurement
import com.influxdb.client.InfluxDBClient
import com.influxdb.client.QueryApi
import com.influxdb.client.WriteApiBlocking
import com.influxdb.client.domain.WritePrecision
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.format.DateTimeFormatter


@Repository
class InfluxDBRepositoryImpl( influxDBClient: InfluxDBClient) : InfluxDBRepository {

    private val writeApi: WriteApiBlocking = influxDBClient.writeApiBlocking
    private val queryApi: QueryApi = influxDBClient.queryApi

    override fun save(measurement: SensorMeasurement) {
        writeApi.writeMeasurement(WritePrecision.MS, measurement)
    }


    override fun findBySensorIdWithin(bucket: String, sensorName: String, durationSec: Long): List<SensorMeasurement> {
        val flux = """
            from(bucket: "$bucket")
            |> range(start: -${durationSec}s)
            |> filter(fn: (r) => r._measurement == "sensor_data")
            |> filter(fn: (r) => r["sensor"] == "$sensorName")
            |> filter(fn: (r) => r._field == "value")
            |> timeShift(duration: 9h)
        """.trimIndent()

        return queryApi.query(flux, SensorMeasurement::class.java)
    }

    override fun findAll(bucket: String): List<SensorMeasurement> {
        val flux = """
            from(bucket: "$bucket")
            |> range(start: 0) 
            |> filter(fn: (r) => r._measurement == "sensor_data")
            |> filter(fn: (r) => r._field == "value")
            |> timeShift(duration: 9h)
        """.trimIndent()

        return queryApi.query(flux, SensorMeasurement::class.java)
    }

    /**
     * ✅ 기간별 조회 (start ~ end)
     */
    override fun findBySensorIdBetween(bucket: String, sensorName: String, start: Instant, end: Instant): List<SensorMeasurement> {
        val startStr = DateTimeFormatter.ISO_INSTANT.format(start)
        val endStr = DateTimeFormatter.ISO_INSTANT.format(end)

        val flux = """
            from(bucket: "$bucket")
            |> range(start: $startStr, stop: $endStr)
            |> filter(fn: (r) => r._measurement == "sensor_data")
            |> filter(fn: (r) => r["sensor"] == "$sensorName")
            |> filter(fn: (r) => r._field == "value")
            |> timeShift(duration: 9h)
        """.trimIndent()

        return queryApi.query(flux, SensorMeasurement::class.java)
    }
}