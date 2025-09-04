package com.example.demo.repository

import com.example.demo.domain.SensorMeasurement
import com.influxdb.client.InfluxDBClient
import com.influxdb.client.QueryApi
import com.influxdb.client.WriteApiBlocking
import com.influxdb.client.domain.WritePrecision
import org.springframework.stereotype.Repository

@Repository
class InfluxDBRepositoryImpl(
    influxDBClient: InfluxDBClient
) : InfluxDBRepository {

    private val writeApi: WriteApiBlocking = influxDBClient.writeApiBlocking
    private val queryApi: QueryApi = influxDBClient.queryApi

    override fun save(measurement: SensorMeasurement) {
        writeApi.writeMeasurement(WritePrecision.MS, measurement)
    }

    override fun findBySensorIdWithin(sensorId: Long, durationSec: Long): List<SensorMeasurement> {
        val flux = """
            from(bucket: "my-bucket")
              |> range(start: -${durationSec}s)
              |> filter(fn: (r) => r._measurement == "sensor_data")
              |> filter(fn: (r) => r["sensorId"] == "${sensorId}")  // ✅ 문자열 비교
              |> filter(fn: (r) => r._field == "value")             // ✅ 값 필드만
        """.trimIndent()

        return queryApi.query(flux, SensorMeasurement::class.java)
    }
}