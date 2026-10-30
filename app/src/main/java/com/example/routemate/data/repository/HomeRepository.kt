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
                        name = d.name,
                        description = d.description,
                        imageRes = mapDestinationImage(d.name)
                    )
                }

                val recommendedRides = feed.recommendedRides.map { r ->
                    RecommendedRide(
                        route = r.route,
                        date = r.date,
                        distance = r.distance,
                        budget = r.budget,
                        riders = r.riders,
                        tripId = r.id.toString(),
                        destination = r.destination,
                        startingPoint = r.startingPoint,
                        duration = r.duration,
                        meetingPoint = r.meetingPoint,
                        title = r.title
                    )
                }

                val nearbyRiders = feed.nearbyRiders.map { n ->
                    NearbyRiderItem(
                        name = n.name,
                        experience = n.experience,
                        destination = n.destination,
                        avatarRes = mapAvatarImage(n.avatarRes),
                        tripId = n.tripId.toString(),
                        dates = n.dates,
                        distance = n.distance,
                        duration = n.duration,
                        budget = n.budget,
                        startingPoint = n.startingPoint,
                        meetingPoint = n.meetingPoint,
                        riderCount = n.riderCount,
                        title = n.title
                    )
                }

                NetworkStateHolder.markOnline()
                Result.success(HomeFeedData(destinations, recommendedRides, nearbyRiders))
            } else {
                throw Exception(response.message ?: "Failed to load home feed")
            }
        } catch (e: Exception) {
            NetworkStateHolder.markOffline(e.localizedMessage ?: "Server unreachable")
            Result.success(LocalMockData.sampleHomeFeed)
        }
    }

    private fun mapDestinationImage(name: String): Int {
        return when (name.lowercase()) {
            "ooty" -> R.drawable.img_ooty
            "coorg" -> R.drawable.img_coorg
            "munnar" -> R.drawable.img_munnar
            "goa" -> R.drawable.img_goa
            "araku valley", "araku" -> R.drawable.img_araku
            else -> R.drawable.img_hero_banner
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
