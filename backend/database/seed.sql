-- RouteMate Development Seed Data
-- Clearly identified sample records for local development and testing

USE `routemate_db`;

-- Disable foreign key checks to allow clean reset
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `notifications`;
TRUNCATE TABLE `learning_resources`;
TRUNCATE TABLE `trip_requests`;
TRUNCATE TABLE `team_members`;
TRUNCATE TABLE `teams`;
TRUNCATE TABLE `trips`;
TRUNCATE TABLE `rider_profiles`;
TRUNCATE TABLE `popular_destinations`;
TRUNCATE TABLE `users`;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. Users (Sample riders matching frontend UI)
-- Password for all seed users is: password123
INSERT INTO `users` (`id`, `email`, `password_hash`, `full_name`, `phone_number`, `age`, `city_state`, `bio`, `avatar_url`) VALUES
(1, 'arjun@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Arjun Kumar', '+91 98765 43210', 28, 'Chennai, Tamil Nadu', 'Passionate motorcycle tourer who loves coastal routes, hill climb bends, and weekend group rides.', 'ic_avatar_1'),
(2, 'meera@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Meera Rao', '+91 98765 43211', 26, 'Vellore, Tamil Nadu', 'Weekend explorer, coffee estate lover, and cruiser enthusiast.', 'ic_avatar_2'),
(3, 'karthik@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Karthik Raja', '+91 98765 43212', 31, 'Coimbatore, Tamil Nadu', 'High-altitude mountain road tourer and motorcycle photographer.', 'ic_avatar_3'),
(4, 'priya@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Priya Sharma', '+91 98765 43213', 24, 'Kanchipuram, Tamil Nadu', 'Beginner rider looking to learn highway safety and join group rides.', 'ic_avatar_4'),
(5, 'rahul@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Rahul Verma', '+91 98765 43214', 29, 'Bengaluru, Karnataka', 'Long-distance highway rider heading to coastal sunsets and beach routes.', 'ic_avatar_1'),
(6, 'sneha@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Sneha Menon', '+91 98765 43215', 27, 'Mysore, Karnataka', 'Lover of misty tea gardens and smooth scenic tarmac.', 'ic_avatar_2'),
(7, 'vikram@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Vikram Patel', '+91 98765 43216', 33, 'Kochi, Kerala', 'Western ghats adventurer and hairpin curves specialist.', 'ic_avatar_3'),
(8, 'aakash@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Aakash Singhania', '+91 98765 43217', 35, 'Delhi, NCR', 'Himalayan pass veteran: Khardung La, Chang La, and Zanskar trails.', 'ic_avatar_1'),
(9, 'tanya@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Tanya Gill', '+91 98765 43218', 30, 'Chandigarh, Punjab', 'Rugged terrain rider, Spiti explorer, and monastery traveler.', 'ic_avatar_4'),
(10, 'kiran@routemate.dev', '$2y$10$HTHsk1W8VXAVIkxaFFl3GeA3SMB4rYdiJlcYPDzFkZ4kVP5TvHeBy', 'Kiran Das', '+91 98765 43219', 28, 'Madurai, Tamil Nadu', 'Pine forest road lover and photography enthusiast.', 'ic_avatar_3');

-- 2. Rider Profiles
INSERT INTO `rider_profiles` (`user_id`, `riding_experience`, `years_experience`, `motorcycle_brand_model`, `motorcycle_cc`, `riding_styles`, `typical_distance`, `preferred_destinations`, `preferred_group_size`, `is_open_to_teams`, `is_willing_to_mentor`, `min_budget`, `max_budget`, `preferred_duration`, `preferred_departure_time`, `travel_pace`, `accommodation_preference`, `starting_city`, `helmet_gear_confirmed`, `emergency_contact_name`, `emergency_contact_phone`, `preferred_language`, `share_live_location`, `agree_safety_guidelines`) VALUES
(1, 'Experienced', 5, 'Royal Enfield Himalayan 450', 452, 'Adventure, Touring, Scenic', 'Medium (100-300 km)', 'Ooty, Pondicherry, Yelagiri, Goa', 'Small (2-5 riders)', 1, 1, 1500, 5000, 'Weekend (2-3 days)', 'Early Morning (5:00 AM)', 'Moderate', 'Standard', 'Chennai', 1, 'Ramesh Kumar', '+91 98765 00001', 'English / Tamil', 1, 1),
(2, 'Intermediate', 3, 'KTM Adventure 390', 373, 'Adventure, Touring', 'Short (<100 km)', 'Yelagiri, Ooty, Coorg', 'Small (2-5 riders)', 1, 0, 1000, 3000, 'Single Day', 'Early Morning (6:00 AM)', 'Moderate', 'Budget Friendly', 'Vellore', 1, 'Anita Rao', '+91 98765 00002', 'English / Tamil', 1, 1),
(3, 'Experienced', 7, 'BMW G310 GS', 313, 'Touring, Scenic', 'Long (>300 km)', 'Ooty, Munnar, Kodaikanal', 'Medium (5-10 riders)', 1, 1, 2500, 6000, 'Weekend (2-3 days)', 'Early Morning (5:30 AM)', 'Brisk & Safe', 'Comfortable', 'Coimbatore', 1, 'Suresh Raja', '+91 98765 00003', 'English / Tamil', 1, 1),
(4, 'Beginner', 1, 'TVS Ronin 225', 225, 'Cruising, Scenic', 'Short (<100 km)', 'Chennai, Mahabalipuram, Pondicherry', 'Small (2-5 riders)', 1, 0, 800, 2000, 'Single Day', 'Morning (7:00 AM)', 'Relaxed', 'Standard', 'Kanchipuram', 1, 'Mohan Sharma', '+91 98765 00004', 'English / Hindi', 1, 1);

-- 3. Trips (Matching the 10 riders in MatchScreen & Recommended Rides in HomeScreen)
INSERT INTO `trips` (`id`, `creator_id`, `title`, `destination`, `starting_point`, `dates`, `distance`, `duration`, `budget`, `meeting_point`, `level_required`, `description`, `rider_count_display`, `max_capacity`, `current_joined`, `status`) VALUES
(1, 1, 'Arjun\'s Pondicherry Coastal Cruise', 'Pondicherry', 'Chennai', 'Nov 8-9', '160 km', '3.5 hrs', 2500, 'ECR Toll Gate, Chennai', 'Experienced', 'Coastal ride and relaxed pace with sunset cafe stops.', '4 / 6 riders', 6, 4, 'open'),
(2, 2, 'Meera\'s Yelagiri Hills & Sunrise Run', 'Yelagiri', 'Vellore', 'Nov 14-15', '90 km', '2.5 hrs', 1800, 'Vellore New Bus Stand', 'Intermediate', 'Hills and sunrise ride with multiple viewpoint stops.', '2 / 5 riders', 5, 2, 'open'),
(3, 3, 'Karthik\'s Ooty Mountain Pass Tour', 'Ooty', 'Coimbatore', 'Nov 20-22', '85 km', '3 hrs', 4500, 'Mettupalayam Junction', 'Experienced', 'Mountain roads, hairpin bend guidance, and photography.', '5 / 6 riders', 6, 5, 'open'),
(4, 4, 'Priya\'s Heritage Day Ride', 'Chennai', 'Kanchipuram', 'Nov 2', '70 km', '1.5 hrs', 900, 'Silk City Highway Plaza', 'Beginner', 'Day ride and easy beginner-friendly pace along bypass.', '1 / 4 riders', 4, 1, 'open'),
(5, 5, 'Rahul\'s Goa Coastal Long Weekend', 'Goa', 'Bengaluru', 'Dec 5-8', '560 km', '10 hrs', 6000, 'Nelamangala Toll Gate', 'Intermediate', 'Beach route, coastal highway exploration, and food stops.', '3 / 6 riders', 6, 3, 'open'),
(6, 6, 'Sneha\'s Coorg Coffee Plantation Trail', 'Coorg', 'Mysore', 'Dec 12-14', '120 km', '3 hrs', 3500, 'Hunsur Road, Mysore', 'Intermediate', 'Coffee estates, misty winding curves, and waterfall visit.', '3 / 5 riders', 5, 3, 'open'),
(7, 7, 'Vikram\'s Munnar Tea Garden Expedition', 'Munnar', 'Kochi', 'Dec 18-20', '130 km', '4 hrs', 5000, 'Aluva Bypass, Kochi', 'Experienced', 'Mist, tea garden panoramas, and challenging hairpins.', '4 / 6 riders', 6, 4, 'open'),
(8, 8, 'Aakash\'s Leh Ladakh High Altitude Odyssey', 'Leh Ladakh', 'Delhi', 'Jun 10-25', '2100 km', '7 days', 35000, 'Kashmir Gate, Delhi', 'Experienced', 'High altitude mountain passes, Khardung La, and adventure trails.', '4 / 8 riders', 8, 4, 'open'),
(9, 9, 'Tanya\'s Spiti Valley Rugged Expedition', 'Spiti Valley', 'Chandigarh', 'Jul 5-15', '750 km', '4 days', 28000, 'Zirakpur, Chandigarh', 'Experienced', 'Rugged Himalayan terrain, ancient monasteries, and camping.', '2 / 6 riders', 6, 2, 'open'),
(10, 10, 'Kiran\'s Kodaikanal Pine Forest Cruise', 'Kodaikanal', 'Madurai', 'Dec 22-24', '120 km', '3 hrs', 4000, 'Mattuthavani, Madurai', 'Intermediate', 'Pine forests, misty curves, and lake road cruising.', '3 / 5 riders', 5, 3, 'open');

-- 4. Teams (Matching TeamsScreen)
INSERT INTO `teams` (`id`, `leader_id`, `name`, `destination`, `starting_point`, `route_itinerary`, `dates`, `distance`, `duration`, `budget`, `members`, `target`, `level_required`, `description`, `meeting_point`, `status`) VALUES
(1, 1, 'Coastal Weekend Crew', 'Pondicherry', 'Chennai', 'Chennai - Mahabalipuram - Pondicherry', 'Nov 8-9', '160 km', '3.5 hrs', 2500, 4, 6, 'All levels', 'Looking for 2 more riders to complete our coastal formation. Scenic cafes and relaxed pace.', 'Mahabalipuram Toll Gate', 'open'),
(2, 2, 'Hills and Coffee Ride', 'Yelagiri', 'Vellore', 'Vellore - Yelagiri', 'Nov 14-15', '90 km', '2.5 hrs', 1800, 3, 5, 'Beginner friendly', 'Friendly pace with designated rest stops. Perfect for first-time hill riders.', 'Vellore Bypass Plaza', 'open'),
(3, 3, 'Nilgiri Adventure', 'Ooty', 'Coimbatore', 'Coimbatore - Coonoor - Ooty', 'Nov 20-22', '85 km', '3 hrs', 4500, 5, 6, 'Intermediate+', 'One seat left. Mountain riding experience preferred. Proper riding gear mandatory.', 'Mettupalayam Junction', 'open'),
(4, 5, 'Goa Long Weekend', 'Goa', 'Bengaluru', 'Bengaluru - Gokarna - Goa', 'Dec 5-8', '560 km', '10 hrs', 6000, 4, 6, 'Intermediate', 'Two riders wanted for coastal highway run. Scenic stops, beach bonfires, and great food.', 'Nelamangala Toll Plaza', 'open');

-- 5. Team Members
INSERT INTO `team_members` (`team_id`, `user_id`, `role`, `status`) VALUES
(1, 1, 'leader', 'approved'),
(1, 4, 'member', 'approved'),
(2, 2, 'leader', 'approved'),
(3, 3, 'leader', 'approved'),
(4, 5, 'leader', 'approved');

-- 6. Learning Resources (Matching LearnScreen)
INSERT INTO `learning_resources` (`icon`, `title`, `summary`, `category`, `display_order`) VALUES
('🏍️', 'Experienced Rider Mentorship', 'Connect with experienced riders to learn about route planning, motorcycle preparation, road conditions and long-distance riding.', 'Mentorship', 1),
('🪪', 'Beginner Rider Guide', 'Start with familiar roads, practise braking and turning, wear protective equipment and ride within your ability.', 'Guide', 2),
('🔧', 'Pre-Ride Checklist', 'Check tyres, brakes, lights, mirrors, chain, fuel, documents and helmet before starting your trip.', 'Safety', 3),
('🗺️', 'Route Planning', 'Plan fuel stops, rest breaks, meeting points, accommodation and alternate routes before departure.', 'Planning', 4),
('👥', 'Group Riding Rules', 'Agree on a lead rider, a sweep rider, hand signals and regrouping points. Maintain a safe distance.', 'Group', 5),
('🌧️', 'Monsoon & Night Riding', 'Reduce speed, increase following distance, wear high-visibility gear and keep headlights clean.', 'Weather', 6),
('🚨', 'Emergency Roadside Response', 'Carry basic tools, puncture kit, first-aid kit and emergency contact numbers on every ride.', 'Emergency', 7);

-- 7. Popular Destinations (Matching HomeScreen)
INSERT INTO `popular_destinations` (`name`, `description`, `image_tag`, `state`) VALUES
('Ooty', 'Misty hills, tea estates & hairpin bends', 'img_ooty', 'Tamil Nadu'),
('Coorg', 'Coffee plantations & lush green waterfalls', 'img_coorg', 'Karnataka'),
('Munnar', 'Endless tea valleys & cool mountain roads', 'img_munnar', 'Kerala'),
('Goa', 'Coastal highways & beach sunsets', 'img_goa', 'Goa'),
('Araku Valley', 'Eastern ghats & scenic valley curves', 'img_araku', 'Andhra Pradesh');

-- 8. Sample Notifications
INSERT INTO `notifications` (`user_id`, `title`, `message`, `type`, `is_read`) VALUES
(1, 'Ride Request Received', 'Arjun, Priya requested to join your Pondicherry ride!', 'ride_invite', 0),
(1, 'Weather Alert', 'Heavy fog predicted along Ooty ghat road between 5 AM - 8 AM.', 'weather_alert', 0),
(1, 'Team Chat', 'Coastal Weekend Crew: Meeting time updated to 5:30 AM at ECR.', 'team_chat', 1);
