USE routemate_db;

-- POPULAR DESTINATIONS
INSERT INTO popular_destinations
(name, state, description)
VALUES
(
    'Ooty',
    'Tamil Nadu',
    'Explore beautiful mountain roads and scenic landscapes.'
),
(
    'Coorg',
    'Karnataka',
    'Enjoy coffee plantations and winding hill roads.'
),
(
    'Munnar',
    'Kerala',
    'Discover tea gardens and mountain scenery.'
),
(
    'Goa',
    'Goa',
    'Experience coastal roads and beautiful beaches.'
),
(
    'Araku Valley',
    'Andhra Pradesh',
    'Explore scenic valleys and beautiful mountain routes.'
);

-- LEARNING RESOURCES
INSERT INTO learning_resources
(title, category, description, content, difficulty)
VALUES
(
    'Motorcycle Riding Basics',
    'Beginner Riding',
    'Learn the fundamentals of motorcycle riding.',
    'Understand balance, braking, clutch control and safe riding practices.',
    'Beginner'
),
(
    'Highway Riding Safety',
    'Road Safety',
    'Learn essential highway riding safety techniques.',
    'Maintain safe distances, follow traffic rules and use protective equipment.',
    'Intermediate'
),
(
    'Long Distance Touring',
    'Touring',
    'Prepare for long motorcycle journeys.',
    'Plan breaks, check your motorcycle and prepare essential travel equipment.',
    'Intermediate'
),
(
    'Group Riding Etiquette',
    'Group Riding',
    'Learn how to ride safely in a motorcycle group.',
    'Maintain formation, communicate with riders and follow group riding rules.',
    'Beginner'
);