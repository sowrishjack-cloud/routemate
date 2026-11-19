package com.example.routemate.data.local

import com.example.routemate.R
import com.example.routemate.data.remote.LearningResourceDto
import com.example.routemate.data.repository.HomeFeedData
import com.example.routemate.models.Rider
import com.example.routemate.models.Team
import com.example.routemate.screens.NearbyRiderItem
import com.example.routemate.screens.PopularDestination
import com.example.routemate.screens.RecommendedRide

object LocalMockData {
    val sampleRiders = listOf(
        Rider(
            id = "1",
            name = "Arjun Kumar",
            destination = "Pondicherry",
            dates = "Nov 8-9",
            level = "Experienced",
            budget = 2500,
            description = "Coastal ride and relaxed pace with sunset cafe stops.",
            startingPoint = "Chennai",
            distance = "160 km",
            duration = "3.5 hrs",
            meetingPoint = "ECR Toll Gate, Chennai"
        ),
        Rider(
            id = "2",
            name = "Meera Rao",
            destination = "Yelagiri",
            dates = "Nov 14-15",
            level = "Intermediate",
            budget = 1800,
            description = "Hills and sunrise ride with multiple viewpoint stops.",
            startingPoint = "Vellore",
            distance = "90 km",
            duration = "2.5 hrs",
            meetingPoint = "Vellore New Bus Stand"
        ),
        Rider(
            id = "3",
            name = "Karthik Raja",
            destination = "Ooty",
            dates = "Nov 20-22",
            level = "Experienced",
            budget = 4500,
            description = "Mountain roads, hairpin bend guidance, and photography.",
            startingPoint = "Coimbatore",
            distance = "85 km",
            duration = "3 hrs",
            meetingPoint = "Mettupalayam Junction"
        ),
        Rider(
            id = "4",
            name = "Priya Sharma",
            destination = "Chennai",
            dates = "Nov 2",
            level = "Beginner",
            budget = 900,
            description = "Day ride and easy beginner-friendly pace along bypass.",
            startingPoint = "Kanchipuram",
            distance = "70 km",
            duration = "1.5 hrs",
            meetingPoint = "Silk City Highway Plaza"
        ),
        Rider(
            id = "5",
            name = "Rahul Verma",
            destination = "Goa",
            dates = "Dec 5-8",
            level = "Intermediate",
            budget = 6000,
            description = "Beach route, coastal highway exploration, and food stops.",
            startingPoint = "Bengaluru",
            distance = "560 km",
            duration = "10 hrs",
            meetingPoint = "Nelamangala Toll Gate"
        ),
        Rider(
            id = "6",
            name = "Sneha Menon",
            destination = "Coorg",
            dates = "Dec 12-14",
            level = "Intermediate",
            budget = 3500,
            description = "Coffee estates, misty winding curves, and waterfall visit.",
            startingPoint = "Mysore",
            distance = "120 km",
            duration = "3 hrs",
            meetingPoint = "Hunsur Road, Mysore"
        ),
        Rider(
            id = "7",
            name = "Vikram Patel",
            destination = "Munnar",
            dates = "Dec 18-20",
            level = "Experienced",
            budget = 5000,
            description = "Mist, tea garden panoramas, and challenging hairpins.",
            startingPoint = "Kochi",
            distance = "130 km",
            duration = "4 hrs",
            meetingPoint = "Aluva Bypass, Kochi"
        ),
        Rider(
            id = "8",
            name = "Aakash Singhania",
            destination = "Leh Ladakh",
            dates = "Jun 10-25",
            level = "Experienced",
            budget = 35000,
            description = "High altitude mountain passes, Khardung La, and adventure trails.",
            startingPoint = "Delhi",
            distance = "2100 km",
            duration = "7 days",
            meetingPoint = "Kashmir Gate, Delhi"
        ),
        Rider(
            id = "9",
            name = "Tanya Gill",
            destination = "Spiti Valley",
            dates = "Jul 5-15",
            level = "Experienced",
            budget = 28000,
            description = "Rugged Himalayan terrain, ancient monasteries, and camping.",
            startingPoint = "Chandigarh",
            distance = "750 km",
            duration = "4 days",
            meetingPoint = "Zirakpur, Chandigarh"
        ),
        Rider(
            id = "10",
            name = "Kiran Das",
            destination = "Kodaikanal",
            dates = "Dec 22-24",
            level = "Intermediate",
            budget = 4000,
            description = "Pine forests, misty curves, and lake road cruising.",
            startingPoint = "Madurai",
            distance = "120 km",
            duration = "3 hrs",
            meetingPoint = "Mattuthavani, Madurai"
        )
    )

    val sampleTeams = listOf(
        Team(
            id = "1",
            name = "Coastal Weekend Crew",
            destination = "Pondicherry",
            dates = "Nov 8-9",
            route = "Chennai - Mahabalipuram - Pondicherry",
            budget = 2500,
            members = 4,
            target = 6,
            level = "All levels",
            description = "Looking for 2 more riders to complete our coastal formation. Scenic cafes and relaxed pace.",
            startingPoint = "Chennai",
            distance = "160 km",
            duration = "3.5 hrs",
            meetingPoint = "Mahabalipuram Toll Gate"
        ),
        Team(
            id = "2",
            name = "Hills and Coffee Ride",
            destination = "Yelagiri",
            dates = "Nov 14-15",
            route = "Vellore - Yelagiri",
            budget = 1800,
            members = 3,
            target = 5,
            level = "Beginner friendly",
            description = "Friendly pace with designated rest stops. Perfect for first-time hill riders.",
            startingPoint = "Vellore",
            distance = "90 km",
            duration = "2.5 hrs",
            meetingPoint = "Vellore Bypass Plaza"
        ),
        Team(
            id = "3",
            name = "Nilgiri Adventure",
            destination = "Ooty",
            dates = "Nov 20-22",
            route = "Coimbatore - Coonoor - Ooty",
            budget = 4500,
            members = 5,
            target = 6,
            level = "Intermediate+",
            description = "One seat left. Mountain riding experience preferred. Proper riding gear mandatory.",
            startingPoint = "Coimbatore",
            distance = "85 km",
            duration = "3 hrs",
            meetingPoint = "Mettupalayam Junction"
        ),
        Team(
            id = "4",
            name = "Goa Long Weekend",
            destination = "Goa",
            dates = "Dec 5-8",
            route = "Bengaluru - Gokarna - Goa",
            budget = 6000,
            members = 4,
            target = 6,
            level = "Intermediate",
            description = "Two riders wanted for coastal highway run. Scenic stops, beach bonfires, and great food.",
            startingPoint = "Bengaluru",
            distance = "560 km",
            duration = "10 hrs",
            meetingPoint = "Nelamangala Toll Plaza"
        )
    )

    val sampleLearning = listOf(
        LearningResourceDto(
            id = 1,
            icon = "🏍️",
            title = "Experienced Rider Mentorship",
            summary = "Connect with experienced riders to learn about route planning, motorcycle preparation, road conditions and long-distance riding.",
            category = "Mentorship",
            displayOrder = 1
        ),
        LearningResourceDto(
            id = 2,
            icon = "🪪",
            title = "Beginner Rider Guide",
            summary = "Start with familiar roads, practise braking and turning, wear protective equipment and ride within your ability.",
            category = "Guide",
            displayOrder = 2
        ),
        LearningResourceDto(
            id = 3,
            icon = "🔧",
            title = "Pre-Ride Checklist",
            summary = "Check tyres, brakes, lights, mirrors, chain, fuel, documents and helmet before starting your trip.",
            category = "Safety",
            displayOrder = 3
        ),
        LearningResourceDto(
            id = 4,
            icon = "🗺️",
            title = "Route Planning",
            summary = "Plan fuel stops, rest breaks, meeting points, accommodation and alternate routes before departure.",
            category = "Planning",
            displayOrder = 4
        ),
        LearningResourceDto(
            id = 5,
            icon = "👥",
            title = "Group Riding Rules",
            summary = "Agree on a lead rider, a sweep rider, hand signals and regrouping points. Maintain a safe distance.",
            category = "Group",
            displayOrder = 5
        ),
        LearningResourceDto(
            id = 6,
            icon = "🌧️",
            title = "Monsoon & Night Riding",
            summary = "Reduce speed, increase following distance, wear high-visibility gear and keep headlights clean.",
            category = "Weather",
            displayOrder = 6
        ),
        LearningResourceDto(
            id = 7,
            icon = "🚨",
            title = "Emergency Roadside Response",
            summary = "Carry basic tools, puncture kit, first-aid kit and emergency contact numbers on every ride.",
            category = "Emergency",
            displayOrder = 7
        )
    )

    val sampleHomeFeed = HomeFeedData(
        destinations = listOf(
            PopularDestination("Ooty", "Misty hills, tea estates & hairpin bends", R.drawable.img_ooty),
            PopularDestination("Coorg", "Coffee plantations & lush green waterfalls", R.drawable.img_coorg),
            PopularDestination("Munnar", "Endless tea valleys & cool mountain roads", R.drawable.img_munnar),
            PopularDestination("Goa", "Coastal highways & beach sunsets", R.drawable.img_goa),
            PopularDestination("Araku Valley", "Eastern ghats & scenic valley curves", R.drawable.img_araku)
        ),
        recommendedRides = listOf(
            RecommendedRide("Chennai → Pondicherry", "Nov 8", "160 km", 2500, "4/6 riders", "1", "Pondicherry", "Chennai", "3.5 hrs", "ECR Toll Gate", "Pondicherry Coastal Cruise"),
            RecommendedRide("Vellore → Yelagiri", "Nov 14", "90 km", 1800, "2/5 riders", "2", "Yelagiri", "Vellore", "2.5 hrs", "Vellore New Bus Stand", "Yelagiri Hills & Sunrise Run"),
            RecommendedRide("Coimbatore → Ooty", "Nov 20", "85 km", 4500, "5/6 riders", "3", "Ooty", "Coimbatore", "3 hrs", "Mettupalayam Junction", "Ooty Mountain Pass Tour")
        ),
        nearbyRiders = listOf(
            NearbyRiderItem("Arjun Kumar", "5 yrs exp • Himalayan 450", "Pondicherry", R.drawable.ic_avatar_1, "1", "Nov 8-9", "160 km", "3.5 hrs", 2500, "Chennai", "ECR Toll Gate", "4 / 6", "Pondicherry Coastal Cruise"),
            NearbyRiderItem("Meera Rao", "3 yrs exp • KTM 390", "Yelagiri", R.drawable.ic_avatar_2, "2", "Nov 14-15", "90 km", "2.5 hrs", 1800, "Vellore", "Vellore New Bus Stand", "2 / 5", "Yelagiri Sunrise Run"),
            NearbyRiderItem("Karthik Raja", "7 yrs exp • BMW G310", "Ooty", R.drawable.ic_avatar_3, "3", "Nov 20-22", "85 km", "3 hrs", 4500, "Coimbatore", "Mettupalayam Junction", "5 / 6", "Ooty Mountain Tour")
        )
    )
}
