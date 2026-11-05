package com.example.routemate.data.repository

import com.example.routemate.R
import com.example.routemate.data.local.LocalMockData
import com.example.routemate.data.remote.NetworkStateHolder
import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.screens.NearbyRiderItem
import com.example.routemate.screens.PopularDestination
import com.example.routemate.screens.RecommendedRide

class HomeRepository {

    suspend fun getHomeFeed(): Result<HomeFeedData> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getHomeFeed()
            if (response.isSuccess && response.data != null) {
                val feed = response.data

                val destinations = feed.destinations.map { d ->
                    PopularDestination(
                        name = d.safeName,
                        description = d.safeDescription,
                        imageRes = mapDestinationImage(d.safeName)
                    )
                }

                val recommendedRides = feed.recommendedRides.map { r ->
                    RecommendedRide(
                        route = r.safeRoute,
                        date = r.safeDate,
                        distance = r.safeDistance,
                        budget = r.safeBudget,
                        riders = r.safeRiders,
                        tripId = r.id.toString(),
                        destination = r.safeDestination,
                        startingPoint = r.startingPoint ?: "Start",
                        duration = r.duration ?: "2 hrs",
                        meetingPoint = r.meetingPoint ?: "Meeting Point",
                        title = r.safeTitle
                    )
                }

                val nearbyRiders = feed.nearbyRiders.map { n ->
                    NearbyRiderItem(
                        name = n.safeName,
                        experience = n.safeExperience,
                        destination = n.safeDestination,
                        avatarRes = mapAvatarImage(n.avatarRes),
                        tripId = (n.tripId ?: 0).toString(),
                        dates = n.safeDates,
                        distance = n.safeDistance,
                        duration = n.duration ?: "2 hrs",
                        budget = n.safeBudget,
                        startingPoint = n.startingPoint ?: "Start",
                        meetingPoint = n.meetingPoint ?: "Meeting Point",
                        riderCount = n.riderCount ?: "1 rider",
                        title = n.title ?: "Ride"
                    )
                }

                NetworkStateHolder.markOnline()
                Result.success(HomeFeedData(destinations, recommendedRides, nearbyRiders))
            } else {
                throw Exception(response.message ?: "Failed to load home feed")
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Failed to connect to backend server"
            NetworkStateHolder.markOffline(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    private fun mapDestinationImage(name: String): Int {
        return when (name.lowercase()) {
            "ooty" -> R.drawable.dest_ooty
            "coorg" -> R.drawable.dest_coorg
            "munnar" -> R.drawable.dest_munnar
            "goa" -> R.drawable.dest_goa
            "araku valley", "araku" -> R.drawable.dest_araku
            "pondicherry", "pondy" -> R.drawable.dest_pondicherry
            "yelagiri" -> R.drawable.dest_yelagiri
            else -> R.drawable.dest_ooty
        }
    }

    private fun mapAvatarImage(res: String?): Int {
        return when (res) {
            "ic_avatar_1" -> R.drawable.ic_avatar_1
            "ic_avatar_2" -> R.drawable.ic_avatar_2
            "ic_avatar_3" -> R.drawable.ic_avatar_3
            "ic_avatar_4" -> R.drawable.ic_avatar_4
            else -> R.drawable.ic_avatar_1
        }
    }
}

data class HomeFeedData(
    val destinations: List<PopularDestination>,
    val recommendedRides: List<RecommendedRide>,
    val nearbyRiders: List<NearbyRiderItem>
)
