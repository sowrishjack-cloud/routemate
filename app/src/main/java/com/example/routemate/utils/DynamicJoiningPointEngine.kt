package com.example.routemate.utils

import android.location.Location
import com.example.routemate.data.remote.CoordinatesDto
import com.example.routemate.data.remote.DynamicRoutingEngine
import com.example.routemate.data.remote.LiveRiderLocationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DynamicJoiningPointResult(
    val meetingCoords: CoordinatesDto,
    val placeName: String,
    val userDistanceKm: Double,
    val userEtaMinutes: Double,
    val partnerDistanceKm: Double,
    val partnerEtaMinutes: Double,
    val partnerName: String,
    val totalRiders: Int,
    val userApproachRoute: List<List<Double>>, // [ [lon, lat], ... ]
    val partnerApproachRoute: List<List<Double>>,
    val commonRouteToDestination: List<List<Double>>,
    val status: JoiningStatus
)

enum class JoiningStatus {
    EN_ROUTE,
    RIDER_ARRIVED,
    ALL_JOINED_RIDING_TOGETHER,
    NO_CONVERGENCE
}

object DynamicJoiningPointEngine {

    /**
     * Dynamically calculates an on-route joining point where multiple riders
     * travelling to the same destination can safely meet along their shared highway corridor.
     */
    suspend fun calculateDynamicJoiningPoint(
        userCoords: CoordinatesDto,
        userSpeed: Float,
        connectedRiders: List<LiveRiderLocationDto>,
        destinationCoords: CoordinatesDto,
        destinationName: String
    ): DynamicJoiningPointResult? = withContext(Dispatchers.IO) {
        // Condition: If there is only one rider (no connected partners), do not display a meeting point
        val validPartners = connectedRiders.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        if (validPartners.isEmpty()) return@withContext null

        if (userCoords.lat == 0.0 && userCoords.lon == 0.0) return@withContext null
        if (destinationCoords.lat == 0.0 && destinationCoords.lon == 0.0) return@withContext null

        val primaryPartner = validPartners.first()
        val partnerCoords = CoordinatesDto(primaryPartner.latitude, primaryPartner.longitude)

        // 1. Fetch User road route to destination
        val userRouteRes = DynamicRoutingEngine.fetchOsrmRoadRoute(listOf(userCoords, destinationCoords))
        val ptsUser = userRouteRes.getOrNull()?.coordinates ?: emptyList()

        // 2. Fetch Partner road route to destination
        val partnerRouteRes = DynamicRoutingEngine.fetchOsrmRoadRoute(listOf(partnerCoords, destinationCoords))
        val ptsPartner = partnerRouteRes.getOrNull()?.coordinates ?: emptyList()

        if (ptsUser.size < 5 || ptsPartner.size < 5) return@withContext null

        // 3. Find the first highway merge / convergence point along the road network
        var mergeUserIdx = -1
        var mergePartnerIdx = -1

        for (i in 2 until ptsUser.size) {
            val uPt = ptsUser[i]
            if (uPt.size < 2) continue

            // Ensure point is ahead of User by at least 250 meters
            val distFromUser = computeDistanceMeters(userCoords.lat, userCoords.lon, uPt[1], uPt[0])
            if (distFromUser < 250f) continue

            // Ensure point is not immediately at the destination door
            val distToDest = computeDistanceMeters(uPt[1], uPt[0], destinationCoords.lat, destinationCoords.lon)
            if (distToDest < 350f) continue

            // Check if any point in partner's route is within merge threshold (150m)
            for (j in 2 until ptsPartner.size) {
                val pPt = ptsPartner[j]
                if (pPt.size < 2) continue

                val distFromPartner = computeDistanceMeters(partnerCoords.lat, partnerCoords.lon, pPt[1], pPt[0])
                if (distFromPartner < 250f) continue

                val mergeGap = computeDistanceMeters(uPt[1], uPt[0], pPt[1], pPt[0])
                if (mergeGap <= 150f) {
                    mergeUserIdx = i
                    mergePartnerIdx = j
                    break
                }
            }
            if (mergeUserIdx != -1) break
        }

        // If no convergence found along the routes
        if (mergeUserIdx == -1 || mergePartnerIdx == -1) {
            return@withContext DynamicJoiningPointResult(
                meetingCoords = destinationCoords,
                placeName = "Destination Approach Junction, $destinationName",
                userDistanceKm = userRouteRes.getOrNull()?.distanceKm ?: 0.0,
                userEtaMinutes = userRouteRes.getOrNull()?.durationMinutes ?: 0.0,
                partnerDistanceKm = partnerRouteRes.getOrNull()?.distanceKm ?: 0.0,
                partnerEtaMinutes = partnerRouteRes.getOrNull()?.durationMinutes ?: 0.0,
                partnerName = primaryPartner.userName,
                totalRiders = validPartners.size + 1,
                userApproachRoute = ptsUser,
                partnerApproachRoute = ptsPartner,
                commonRouteToDestination = emptyList(),
                status = JoiningStatus.NO_CONVERGENCE
            )
        }

        // Advance slightly along the merged highway (approx 6-10 points / 600m - 1.2km)
        // to select a safe roadside joining area (rest stop, fuel station, or highway junction)
        val safeMeetingIdx = (mergeUserIdx + 8).coerceAtMost(ptsUser.size - 2)
        val meetingPointCoord = ptsUser[safeMeetingIdx]
        val meetLon = meetingPointCoord[0]
        val meetLat = meetingPointCoord[1]
        val meetingCoordsDto = CoordinatesDto(meetLat, meetLon)

        // 4. Reverse-geocode to get a descriptive highway / town landmark
        val placeName = DynamicRoutingEngine.reverseGeocode(meetLat, meetLon)

        // 5. Slice route segments
        val userApproach = ptsUser.subList(0, (safeMeetingIdx + 1).coerceAtMost(ptsUser.size))
        val partnerApproach = ptsPartner.subList(0, (mergePartnerIdx + 1).coerceAtMost(ptsPartner.size))
        val commonRoute = ptsUser.subList(safeMeetingIdx, ptsUser.size)

        // 6. Compute approach distances & ETAs
        val userDistMeters = computeRouteDistanceMeters(userApproach)
        val partnerDistMeters = computeRouteDistanceMeters(partnerApproach)

        val userDistKm = userDistMeters / 1000.0
        val partnerDistKm = partnerDistMeters / 1000.0

        val effUserSpeed = if (userSpeed >= 20f) userSpeed.toDouble() else 55.0
        val effPartnerSpeed = if (primaryPartner.speed >= 20f) primaryPartner.speed.toDouble() else 55.0

        val userEtaMins = (userDistKm / effUserSpeed) * 60.0
        val partnerEtaMins = (partnerDistKm / effPartnerSpeed) * 60.0

        // 7. Arrival Detection
        val userArrived = userDistMeters <= 250f
        val partnerArrived = partnerDistMeters <= 250f

        val currentStatus = when {
            userArrived && partnerArrived -> JoiningStatus.ALL_JOINED_RIDING_TOGETHER
            userArrived || partnerArrived -> JoiningStatus.RIDER_ARRIVED
            else -> JoiningStatus.EN_ROUTE
        }

        DynamicJoiningPointResult(
            meetingCoords = meetingCoordsDto,
            placeName = placeName,
            userDistanceKm = userDistKm,
            userEtaMinutes = userEtaMins,
            partnerDistanceKm = partnerDistKm,
            partnerEtaMinutes = partnerEtaMins,
            partnerName = primaryPartner.userName,
            totalRiders = validPartners.size + 1,
            userApproachRoute = userApproach,
            partnerApproachRoute = partnerApproach,
            commonRouteToDestination = commonRoute,
            status = currentStatus
        )
    }

    fun computeDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val res = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, res)
        return res[0]
    }

    private fun computeRouteDistanceMeters(points: List<List<Double>>): Float {
        var total = 0f
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (p1.size >= 2 && p2.size >= 2) {
                total += computeDistanceMeters(p1[1], p1[0], p2[1], p2[0])
            }
        }
        return total
    }
}
