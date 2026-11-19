<?php
declare(strict_types=1);

namespace RouteMate\Services;

use RouteMate\Config\Database;
use PDO;

class RouteAttractionDiscoveryEngine {

    /**
     * Comprehensive curated directory of on-route highway corridor attractions,
     * historical forts, ghat passes, waterfalls, lakes, viewpoints, and motorcycle food stops across India.
     */
    private const CORRIDOR_PLACES = [
        // --- CHENNAI to OOTY / COIMBATORE / KODAIKANAL CORRIDOR (NH48 / NH79 / NH544 / NH181) ---
        [
            'name' => 'Mahabalipuram Shore Temples & Pancha Rathas',
            'category' => 'Historical places',
            'icon' => '🏛️',
            'description' => 'UNESCO World Heritage coastal monolithic rock sculptures & 7th-century Shore Temple along ECR.',
            'lat' => 12.6269, 'lon' => 80.1927,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '1.5 - 2 hrs',
            'best_time_to_visit' => '6:00 AM - 10:00 AM / Sunrise',
            'entry_fee' => '₹40 (Indian)',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Scenic coastal ride via ECR with cool morning sea breeze. Dedicated motorcycle parking at entrance.'
        ],
        [
            'name' => 'Kanchipuram Heritage & Silk Town',
            'category' => 'Historical places',
            'icon' => '🛕',
            'description' => 'City of a thousand temples featuring Kailasanathar and Ekambareswarar ancient Pallava stone architecture.',
            'lat' => 12.8342, 'lon' => 79.7036,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '1 - 2 hrs',
            'best_time_to_visit' => 'Morning / Evening',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Great breakfast stop (idli/vada) for Chennai departures. Watch for temple lane pedestrian traffic.'
        ],
        [
            'name' => 'Gingee Fort (Troy of the East)',
            'category' => 'Historical places',
            'icon' => '🏰',
            'description' => 'Spectacular hill citadel built over three rocky hills (Rajagiri, Krishnagiri) with 800-foot granite cliffs.',
            'lat' => 12.2544, 'lon' => 79.4189,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '2 - 3 hrs',
            'best_time_to_visit' => 'October - March (Morning)',
            'entry_fee' => '₹25',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Challenging steep stair climb. Park motorcycle safely at the ASI ticket counter.'
        ],
        [
            'name' => 'Villupuram Highway Rider Refreshment Stop',
            'category' => 'Food',
            'icon' => '🍽️',
            'description' => 'Major transit intersection with 24/7 highway diners, South Indian filter coffee, and tier-1 fuel stations.',
            'lat' => 11.9401, 'lon' => 79.4861,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '30 - 45 mins',
            'best_time_to_visit' => 'Anytime',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Top up fuel tank here. Quick rest stop before entering central Tamil Nadu plains.'
        ],
        [
            'name' => 'Yercaud Shevaroy Hills Ghat Road',
            'category' => 'Mountains',
            'icon' => '⛰️',
            'description' => '20 winding hairpin bends ascending to 1,515m overlooking Salem city, coffee groves, and orange orchards.',
            'lat' => 11.7753, 'lon' => 78.2093,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '2 - 3 hrs',
            'best_time_to_visit' => 'September - March',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Exhilarating hill climb with pristine tarmac. Watch out for monkeys on corner apexes.'
        ],
        [
            'name' => 'Bhavani Kooduthurai (Sangameswarar Temple)',
            'category' => 'Temples',
            'icon' => '🛕',
            'description' => 'Sacred confluence of Cauvery, Bhavani, and subterranean Amudha rivers known as the Dakshina Triveni Sangam.',
            'lat' => 11.4485, 'lon' => 77.6835,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '45 mins',
            'best_time_to_visit' => 'Early morning',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Serene river breezes. Ample roadside parking along the NH544 bypass bridge.'
        ],
        [
            'name' => 'Mettupalayam Nilgiri Mountain Railway Foothills',
            'category' => 'Nature',
            'icon' => '🚂',
            'description' => 'Base station of the UNESCO Nilgiri toy train and entry point to the scenic Kotagiri/Coonoor mountain ghat roads.',
            'lat' => 11.3000, 'lon' => 76.9500,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '30 mins',
            'best_time_to_visit' => 'Morning',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Check tire pressure and cool your engine here before ascending the 14-hairpin mountain climb.'
        ],
        [
            'name' => 'Kallatty Ghat Road (36 Hairpin Bends)',
            'category' => 'Adventure',
            'icon' => '🏍️',
            'description' => 'Legendary steep mountain descent with 36 extreme hairpin bends linking Ooty to Masinagudi and Bandipur.',
            'lat' => 11.5167, 'lon' => 76.6833,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '1.5 hrs',
            'best_time_to_visit' => 'Daylight only (6 AM - 6 PM)',
            'entry_fee' => 'Free (Police checkpost)',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Extreme gradient! Keep in low gear (1st/2nd) on descent. Engine braking is vital to prevent brake fade.'
        ],
        [
            'name' => 'Sim\'s Park & Dolphin\'s Nose (Coonoor)',
            'category' => 'Viewpoints',
            'icon' => '🏞️',
            'description' => 'Enormous natural rock viewpoint resembling a dolphin\'s nose offering views of Catherine Falls and Nilgiri tea estates.',
            'lat' => 11.3530, 'lon' => 76.7959,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '1 - 2 hrs',
            'best_time_to_visit' => 'Morning mist (8 AM - 11 AM)',
            'entry_fee' => '₹30',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Road from Coonoor passes through lush green high-altitude tea gardens with smooth tarmac.'
        ],
        [
            'name' => 'Pykara Waterfalls & Lake',
            'category' => 'Waterfalls',
            'icon' => '🌊',
            'description' => 'Sacred Toda river cascading into two step falls amidst pine forests and rolling Nilgiri shola grasslands.',
            'lat' => 11.4500, 'lon' => 76.6000,
            'state' => 'Tamil Nadu',
            'visiting_duration' => '1 hr',
            'best_time_to_visit' => 'August - January',
            'entry_fee' => '₹20',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Located 21 km from Ooty on Mysore road. Great spot for photography and fresh mountain air.'
        ],

        // --- BENGALURU to COORG / WAYANAD / GOA CORRIDOR ---
        [
            'name' => 'Srirangapatna Island Fortress & Ranganathaswamy',
            'category' => 'Historical places',
            'icon' => '🏰',
            'description' => 'Historic river-island citadel of Tipu Sultan enclosed by the Cauvery River, with dungeons and summer palace.',
            'lat' => 12.4239, 'lon' => 76.6841,
            'state' => 'Karnataka',
            'visiting_duration' => '1 - 1.5 hrs',
            'best_time_to_visit' => 'Morning',
            'entry_fee' => '₹25',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Directly off the Bengaluru-Mysuru Expressway service road. Great pitstop before heading into the hills.'
        ],
        [
            'name' => 'Bylakuppe Golden Temple (Namdroling Monastery)',
            'category' => 'Heritage',
            'icon' => '🏯',
            'description' => 'Second largest Tibetan settlement outside Tibet, featuring magnificent 40-foot gold-plated Buddha statues.',
            'lat' => 12.4286, 'lon' => 75.9675,
            'state' => 'Karnataka',
            'visiting_duration' => '1 - 1.5 hrs',
            'best_time_to_visit' => '9:00 AM - 5:00 PM',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Right on Mysore-Madikeri highway. Try authentic Tibetan momos and thukpa outside the monastery gate.'
        ],
        [
            'name' => 'Dubare Elephant Camp & Cauvery River Crossing',
            'category' => 'Wildlife',
            'icon' => '🐘',
            'description' => 'Forest camp on the banks of Cauvery river where elephants are bathed and groomed in their natural habitat.',
            'lat' => 12.3686, 'lon' => 75.9042,
            'state' => 'Karnataka',
            'visiting_duration' => '2 hrs',
            'best_time_to_visit' => 'Early morning (8:30 AM - 10:30 AM)',
            'entry_fee' => '₹100 (including boat ride)',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Forest road leading to Dubare is lined with bamboo thickets and teak trees. Keep speeds moderate.'
        ],
        [
            'name' => 'Abbey Falls (Madikeri)',
            'category' => 'Waterfalls',
            'icon' => '🌊',
            'description' => 'Spectacular 70-foot waterfall roaring amidst dense private coffee plantations and spice estates.',
            'lat' => 12.4542, 'lon' => 75.7175,
            'state' => 'Karnataka',
            'visiting_duration' => '1 hr',
            'best_time_to_visit' => 'July - December',
            'entry_fee' => '₹15',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Narrow winding approach road through coffee estates. 5-minute easy downhill walk to the hanging bridge.'
        ],
        [
            'name' => 'Chitradurga Fort (Seven Hills Fortress)',
            'category' => 'Historical places',
            'icon' => '🏰',
            'description' => 'Massive stone fortress with 7 concentric defensive walls, 38 rear gateways, and 18 ancient temples on NH48.',
            'lat' => 14.2181, 'lon' => 76.4000,
            'state' => 'Karnataka',
            'visiting_duration' => '2 hrs',
            'best_time_to_visit' => 'October - February (Early Morning)',
            'entry_fee' => '₹25',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Perfect mid-way halt on Bengaluru-Goa or Bengaluru-Pune ride. Historic stone architecture.'
        ],
        [
            'name' => 'Jog Falls (Gersoppa)',
            'category' => 'Waterfalls',
            'icon' => '🌊',
            'description' => 'Second highest plunge waterfall in India (253m) formed by Sharavathi river cascading down 4 distinct falls.',
            'lat' => 14.2289, 'lon' => 74.8122,
            'state' => 'Karnataka',
            'visiting_duration' => '2 hrs',
            'best_time_to_visit' => 'July - November (Monsoon peak)',
            'entry_fee' => '₹50',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Scenic detour on Bengaluru-Goa highway via Shimoga. Spectacular viewpoint for photographers.'
        ],
        [
            'name' => 'Agumbe Sunset Point & Rainforest Ghat',
            'category' => 'Mountains',
            'icon' => '⛰️',
            'description' => 'Cherrapunji of the South. Dramatic 14 hairpin bends through dense Western Ghats canopy down to Arabian Sea.',
            'lat' => 13.5042, 'lon' => 75.0931,
            'state' => 'Karnataka',
            'visiting_duration' => '1.5 hrs',
            'best_time_to_visit' => 'Sunset (5:00 PM - 6:30 PM)',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'One of the finest motorcycle riding routes in South India. Watch for heavy mist and wet leaves in monsoon.'
        ],

        // --- DELHI / CHANDIGARH to MANALI / LEH LADAKH CORRIDOR (NH44 / NH3 / NH1) ---
        [
            'name' => 'Murthal Highway Dhaba Corridor',
            'category' => 'Food',
            'icon' => '🍽️',
            'description' => 'Iconic 24/7 North Indian highway food landmark famous for piping hot tandoori paranthas with fresh white butter.',
            'lat' => 29.0256, 'lon' => 77.0694,
            'state' => 'Haryana',
            'visiting_duration' => '45 mins',
            'best_time_to_visit' => '4:00 AM - 9:00 AM (Early departure breakfast)',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'The traditional launching pad for all Delhi motorcycle expeditions heading to Ladakh and Himachal.'
        ],
        [
            'name' => 'Pandoh Dam & Beas River Gorge',
            'category' => 'Nature',
            'icon' => '🌊',
            'description' => 'Striking embankment dam on the Beas river creating a scenic turquoise reservoir along the Mandi-Kullu highway.',
            'lat' => 31.6700, 'lon' => 77.0500,
            'state' => 'Himachal Pradesh',
            'visiting_duration' => '20 mins',
            'best_time_to_visit' => 'Daylight',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Roadside view-point for quick hydration and photos of the emerald water spillway.'
        ],
        [
            'name' => 'Kullu Valley River Rafting Point',
            'category' => 'Adventure',
            'icon' => '🚣',
            'description' => 'Vibrant adventure hub on Beas river with white-water rapids, apple orchards, and handicraft shawls.',
            'lat' => 31.9579, 'lon' => 77.1095,
            'state' => 'Himachal Pradesh',
            'visiting_duration' => '1 - 2 hrs',
            'best_time_to_visit' => 'March - June, September - November',
            'entry_fee' => 'Activity charge approx ₹600 - ₹1200',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Smooth 4-lane Himalayan expressway corridor before ascending to Manali.'
        ],
        [
            'name' => 'Atal Tunnel (Rohtang Bypass)',
            'category' => 'Mountains',
            'icon' => '🏔️',
            'description' => 'World\'s longest highway tunnel above 10,000 feet (9.02 km), connecting Kullu Valley to high desert Lahaul.',
            'lat' => 32.3639, 'lon' => 77.1681,
            'state' => 'Himachal Pradesh',
            'visiting_duration' => '30 mins',
            'best_time_to_visit' => 'May - November',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Speed limit is 60 km/h strictly enforced. Dramatic climate shift from green valley to snowy desert within 10 minutes!'
        ],
        [
            'name' => 'Sissu Waterfall & Palden Lhamo Dhar',
            'category' => 'Waterfalls',
            'icon' => '🌊',
            'description' => 'Glacial melt waterfall plunging 50m down sheer cliffs opposite Sissu village with poplar trees and Lahauli houses.',
            'lat' => 32.4833, 'lon' => 77.1167,
            'state' => 'Himachal Pradesh',
            'visiting_duration' => '1 hr',
            'best_time_to_visit' => 'May - October',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Right outside the North Portal of Atal Tunnel. Perfect tea and Maggi stop for touring riders.'
        ],
        [
            'name' => 'Keylong & Kardang Monastery',
            'category' => 'Heritage',
            'icon' => '🏯',
            'description' => 'District headquarters of Lahaul & Spiti with 900-year-old Drukpa Kagyu monastery facing the snow-capped peak of Beeling.',
            'lat' => 32.5714, 'lon' => 77.0322,
            'state' => 'Himachal Pradesh',
            'visiting_duration' => '1 hr',
            'best_time_to_visit' => 'June - October',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Crucial fuel station here (Tandi Fuel Pump is the last petrol pump for 365 km until Karu near Leh!).'
        ],
        [
            'name' => 'Deepak Tal & Suraj Tal (Lake of the Sun God)',
            'category' => 'High-Altitude Lake',
            'icon' => '💎',
            'description' => 'High altitude glacial lakes at 3,750m and 4,883m reflecting the Baralacha mountain peaks with crystal clarity.',
            'lat' => 32.7483, 'lon' => 77.3986,
            'state' => 'Himachal Pradesh',
            'visiting_duration' => '30 mins',
            'best_time_to_visit' => 'June - September',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'High altitude zone (>4,500m). Keep physical exertion minimal and stay hydrated.'
        ],
        [
            'name' => 'Baralacha La Pass (4,890m)',
            'category' => 'Mountain Pass',
            'icon' => '🏔️',
            'description' => 'High mountain pass in the Zanskar range where trails from Zanskar, Ladakh, Spiti, and Lahaul meet.',
            'lat' => 32.7567, 'lon' => 77.4208,
            'state' => 'Himachal Pradesh',
            'visiting_duration' => '20 mins',
            'best_time_to_visit' => 'June - September (Midday)',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Crossing water nallas (Zingzingbar) early morning is safer when snowmelt volume is low.'
        ],
        [
            'name' => 'Gata Loops (21 Hairpin Mountain Bends)',
            'category' => 'Adventure',
            'icon' => '🏍️',
            'description' => 'Legendary 21 consecutive hairpin curves climbing from 4,190m to Nakee La (4,739m) on the Manali-Leh Highway.',
            'lat' => 32.9667, 'lon' => 77.5833,
            'state' => 'Ladakh',
            'visiting_duration' => '45 mins',
            'best_time_to_visit' => 'June - September',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'One of the most celebrated motorcycling stretches in the world. Drink plenty of water and don\'t stop on corners.'
        ],
        [
            'name' => 'More Plains (Kiang Chu Thang)',
            'category' => 'Adventure Route',
            'icon' => '🛣️',
            'description' => '40 km of flat, wide high-altitude plateau at 4,800m flanked by rolling multi-colored mountain ranges.',
            'lat' => 33.1500, 'lon' => 77.6500,
            'state' => 'Ladakh',
            'visiting_duration' => '1 hr',
            'best_time_to_visit' => 'June - September',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Smooth blacktop highway on high plateau. Spot wild Tibetan asses (Kiangs) grazing in the distance.'
        ],
        [
            'name' => 'Tanglang La Pass (5,328m)',
            'category' => 'Mountain Pass',
            'icon' => '🏔️',
            'description' => 'Second highest motorable pass on the Manali-Leh Highway offering panoramic views of the Ladakh range.',
            'lat' => 33.5083, 'lon' => 77.7708,
            'state' => 'Ladakh',
            'visiting_duration' => '20 mins',
            'best_time_to_visit' => 'June - September (Noon)',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Severe altitude (5,328m). Spend no more than 15-20 minutes at the summit sign board.'
        ],

        // --- MUMBAI to GOA (NH66 / Coastal Konkan Highway) ---
        [
            'name' => 'Karnala Bird Sanctuary & Funnel Hill Fort',
            'category' => 'Nature',
            'icon' => '🦅',
            'description' => 'Forested sanctuary around a 12th-century pinnacle fort right off the Mumbai-Goa Highway (NH66).',
            'lat' => 18.8892, 'lon' => 73.1189,
            'state' => 'Maharashtra',
            'visiting_duration' => '1 hr',
            'best_time_to_visit' => 'Morning',
            'entry_fee' => '₹35',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Pleasant morning ride out of Mumbai/Navi Mumbai with shaded forest road.'
        ],
        [
            'name' => 'Kolad Kundalika River Rapids',
            'category' => 'Adventure',
            'icon' => '🌊',
            'description' => 'Only white-water rafting facility in Maharashtra, fed by water release from Bhira Dam.',
            'lat' => 18.4239, 'lon' => 73.3325,
            'state' => 'Maharashtra',
            'visiting_duration' => '2 hrs',
            'best_time_to_visit' => 'Monsoon & Winter',
            'entry_fee' => 'Rafting fee approx ₹800',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Riders love the twisting roads around Kolad leading towards Mangaon and Tamhini Ghat.'
        ],
        [
            'name' => 'Kashedi Ghat Mountain Pass',
            'category' => 'Mountains',
            'icon' => '⛰️',
            'description' => 'Scenic mountain pass on NH66 with views of deep Konkan valleys and seasonal cascading waterfalls.',
            'lat' => 17.8833, 'lon' => 73.4500,
            'state' => 'Maharashtra',
            'visiting_duration' => '30 mins',
            'best_time_to_visit' => 'July - February',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'New bypass tunnel is available, but the old ghat road remains a favorite for scenic motorcycling curves.'
        ],
        [
            'name' => 'Ganpatipule Beach & Temple',
            'category' => 'Beaches',
            'icon' => '🏖️',
            'description' => '400-year-old self-manifested Ganesha temple on the pristine white sand beach of the Konkan coast.',
            'lat' => 17.1478, 'lon' => 73.2678,
            'state' => 'Maharashtra',
            'visiting_duration' => '1.5 hrs',
            'best_time_to_visit' => 'Sunset',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Aare Ware coastal cliff road connecting Ganpatipule to Ratnagiri is one of India\'s best coastal drives.'
        ],
        [
            'name' => 'Sindhudurg Fort (Malvan)',
            'category' => 'Historical places',
            'icon' => '🏰',
            'description' => 'Impregnable island naval fort built by Chhatrapati Shivaji Maharaj in 1664 in the Arabian Sea.',
            'lat' => 16.0400, 'lon' => 73.4600,
            'state' => 'Maharashtra',
            'visiting_duration' => '2 hrs',
            'best_time_to_visit' => 'October - March',
            'entry_fee' => '₹50 + Ferry ride',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Take the scenic coastal state highway detour via Malvan before crossing into North Goa.'
        ],

        // --- RISHIKESH to KEDARNATH / BADRINATH / CHOPTA (Char Dham Highway) ---
        [
            'name' => 'Devprayag (Alaknanda & Bhagirathi Confluence)',
            'category' => 'Nature',
            'icon' => '🌊',
            'description' => 'The divine confluence where Bhagirathi and Alaknanda rivers meet to officially become the Holy Ganga.',
            'lat' => 30.1458, 'lon' => 78.5986,
            'state' => 'Uttarakhand',
            'visiting_duration' => '45 mins',
            'best_time_to_visit' => 'Morning',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Spectacular viewpoint from the suspension bridge where the two distinctly colored rivers merge.'
        ],
        [
            'name' => 'Rudraprayag (Mandakini & Alaknanda Sangam)',
            'category' => 'Nature',
            'icon' => '🌊',
            'description' => 'Key crossroads where the road splits towards Kedarnath (via Mandakini) and Badrinath (via Alaknanda).',
            'lat' => 30.2850, 'lon' => 78.9817,
            'state' => 'Uttarakhand',
            'visiting_duration' => '30 mins',
            'best_time_to_visit' => 'Daylight',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Essential overnight or fuel stop. Well-paved highway with dramatic river canyon views.'
        ],
        [
            'name' => 'Karnaprayag (Pindar & Alaknanda Sangam)',
            'category' => 'Nature',
            'icon' => '🏞️',
            'description' => 'Gateway to Kumaon hills (Gwaldam, Ranikhet) and base for trekking to Roopkund lake.',
            'lat' => 30.2600, 'lon' => 79.2200,
            'state' => 'Uttarakhand',
            'visiting_duration' => '30 mins',
            'best_time_to_visit' => 'Daylight',
            'entry_fee' => 'Free',
            'motorcycle_friendly' => true,
            'rider_tip' => 'Great highway stop with multiple roadside mechanic stalls and puncture repair shops.'
        ]
    ];

    /**
     * Discover all attractions along a route corridor between origin and destination
     */
    public function discoverCorridorAttractions(
        string $originName,
        float $originLat,
        float $originLon,
        string $destName,
        float $destLat,
        float $destLon,
        array $routeCoordinates = [],
        float $totalDistanceKm = 0.0
    ): array {
        $db = Database::getConnection();

        // 1. Fetch all candidate destinations from popular_destinations table
        $curatedStmt = $db->query('
            SELECT id, name, subtitle, description, state, region, category, latitude, longitude, altitude_meters, difficulty, image_tag, image_url, permit_required
            FROM popular_destinations
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        ');
        $dbDestinations = $curatedStmt->fetchAll(PDO::FETCH_ASSOC);

        // Normalize DB destinations into common attraction schema
        $candidates = [];
        foreach ($dbDestinations as $d) {
            $candidates[] = [
                'name' => $d['name'],
                'subtitle' => $d['subtitle'] ?? '',
                'category' => $d['category'] ?? 'Mountains',
                'icon' => $this->getCategoryIcon($d['category'] ?? ''),
                'description' => $d['description'],
                'lat' => (float)$d['latitude'],
                'lon' => (float)$d['longitude'],
                'state' => $d['state'],
                'region' => $d['region'],
                'altitude_meters' => (int)($d['altitude_meters'] ?? 0),
                'difficulty' => $d['difficulty'] ?? 'Moderate',
                'visiting_duration' => (int)($d['altitude_meters'] ?? 0) >= 3000 ? '1 - 2 hrs' : '45 mins - 1 hr',
                'best_time_to_visit' => 'Morning hours',
                'entry_fee' => !empty($d['permit_required']) && $d['permit_required'] !== 'None' ? 'Permit / Entry pass' : 'Free',
                'motorcycle_friendly' => true,
                'rider_tip' => "Famous motorcycle touring highlight in {$d['state']} with mountain scenery.",
                'image_tag' => $d['image_tag'] ?? 'ic_mountain',
                'is_curated_db' => true
            ];
        }

        // Add highway corridor POIs
        foreach (self::CORRIDOR_PLACES as $cp) {
            $candidates[] = array_merge($cp, [
                'subtitle' => "Scenic Highway Attraction in {$cp['state']}",
                'region' => $cp['state'],
                'altitude_meters' => 0,
                'difficulty' => 'Moderate',
                'image_tag' => 'ic_mountain',
                'is_curated_db' => false
            ]);
        }

        // 2. Sample route polyline points to check corridor proximity
        $samplePoints = [];
        if (!empty($routeCoordinates)) {
            $step = max(1, (int)floor(count($routeCoordinates) / 100));
            for ($i = 0; $i < count($routeCoordinates); $i += $step) {
                // Route coordinates are in [lon, lat] GeoJSON format
                $lon = (float)$routeCoordinates[$i][0];
                $lat = (float)$routeCoordinates[$i][1];
                $samplePoints[] = ['lat' => $lat, 'lon' => $lon, 'index' => $i];
            }
        } else {
            // Direct interpolation between origin and destination
            for ($t = 0.0; $t <= 1.0; $t += 0.05) {
                $samplePoints[] = [
                    'lat' => $originLat + $t * ($destLat - $originLat),
                    'lon' => $originLon + $t * ($destLon - $originLon),
                    'index' => (int)round($t * 100)
                ];
            }
        }

        // 3. Filter candidates within 45 km corridor of the route
        $matched = [];
        $seenNames = [];

        foreach ($candidates as $cand) {
            $candName = strtolower(trim($cand['name']));
            if (isset($seenNames[$candName])) {
                continue;
            }

            // Calculate minimum distance from the sampled route points
            $minDistKm = 999999.0;
            $bestSampleIndex = 0;

            foreach ($samplePoints as $sp) {
                $d = $this->haversineKm($cand['lat'], $cand['lon'], $sp['lat'], $sp['lon']);
                if ($d < $minDistKm) {
                    $minDistKm = $d;
                    $bestSampleIndex = $sp['index'];
                }
            }

            // Exclude exact origin and destination themselves
            $distToOrigin = $this->haversineKm($cand['lat'], $cand['lon'], $originLat, $originLon);
            $distToDest = $this->haversineKm($cand['lat'], $cand['lon'], $destLat, $destLon);

            // Corridor match threshold: <= 40 km from route
            if ($minDistKm <= 40.0) {
                $seenNames[$candName] = true;
                $detourKm = round($minDistKm * 2, 1);
                $extraTimeMins = (int)round(($detourKm / 45.0) * 60);

                $cand['distance_from_route_km'] = round($minDistKm, 1);
                $cand['detour_distance_km'] = $detourKm;
                $cand['detour_time_minutes'] = $extraTimeMins;
                $cand['detour_display'] = $minDistKm < 3.0 ? 'Directly on Highway (0 km Detour)' : "+{$detourKm} km Detour (~{$extraTimeMins} mins)";
                $cand['route_progress_pct'] = !empty($routeCoordinates) ? round(($bestSampleIndex / count($routeCoordinates)) * 100) : 50;
                $cand['is_on_route'] = $minDistKm < 3.0;

                $matched[] = $cand;
            }
        }

        // 4. Sort matched attractions sequentially along the route progress from origin to destination
        usort($matched, fn($a, $b) => $a['route_progress_pct'] <=> $b['route_progress_pct']);

        // Limit to top 15 most relevant corridor stops to keep payload optimal
        $matched = array_slice($matched, 0, 15);

        // Group into segments: Departure area, Highway & valley stops, Ghat & destination stops
        $departure = [];
        $intermediate = [];
        $mountainGhat = [];

        foreach ($matched as $stop) {
            if ($stop['route_progress_pct'] <= 25) {
                $departure[] = $stop;
            } elseif ($stop['route_progress_pct'] >= 75 || ($stop['altitude_meters'] ?? 0) >= 1500 || $stop['category'] === 'Mountains' || $stop['category'] === 'Mountain Pass') {
                $mountainGhat[] = $stop;
            } else {
                $intermediate[] = $stop;
            }
        }

        return [
            'total_stops_found' => count($matched),
            'corridor_stops' => $matched,
            'grouped_by_segment' => [
                'departure_stops' => $departure,
                'intermediate_highway_stops' => $intermediate,
                'mountain_and_destination_stops' => $mountainGhat
            ]
        ];
    }

    private function getCategoryIcon(string $cat): string {
        return match (strtolower($cat)) {
            'mountain pass', 'mountains', 'mountain & hill' => '⛰️',
            'high-altitude lake', 'lake' => '💎',
            'valley' => '🌲',
            'waterfalls' => '🌊',
            'beach', 'coastal' => '🏖️',
            'heritage', 'historical places' => '🏛️',
            'temples' => '🛕',
            'food' => '🍽️',
            'fuel' => '⛽',
            'adventure', 'adventure route' => '🏍️',
            'wildlife' => '🐅',
            default => '📍'
        };
    }

    private function haversineKm(float $lat1, float $lon1, float $lat2, float $lon2): float {
        $earthRadius = 6371.0;
        $dLat = deg2rad($lat2 - $lat1);
        $dLon = deg2rad($lon2 - $lon1);
        $a = sin($dLat / 2) * sin($dLat / 2) +
            cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
            sin($dLon / 2) * sin($dLon / 2);
        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
        return $earthRadius * $c;
    }
}
