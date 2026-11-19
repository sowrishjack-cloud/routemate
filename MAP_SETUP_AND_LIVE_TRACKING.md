# RouteMate map configuration notes

## Map provider priority

The existing MapLibre implementation now prefers the CARTO Voyager style when `CARTO_API_KEY` is configured. MapTiler remains a fallback only when CARTO is not configured.

## Add your CARTO key

Open `local.properties` and set:

```properties
CARTO_API_KEY=YOUR_NEW_CARTO_API_KEY
```

The API keys from the uploaded archive were intentionally removed from this copy. Because the key was visible in a screenshot/chat, regenerate it in your CARTO account before using it again. Do not commit `local.properties` to Git.

## What this update changes

- Uses the CARTO key first, instead of selecting a configured MapTiler key first.
- Keeps MapLibre and the existing map screen.
- Keeps existing start/destination markers, route line, zoom controls and attribution.

## Important: live rider locations are not yet implemented

The uploaded project has static trip start/destination markers and enables MapLibre's own-location component when Android location permission is already granted. It does not currently have authenticated backend endpoints to publish a rider's GPS position to a trip/team and retrieve other participants' latest positions.

A true multi-rider live map still needs:
1. A trip/team-scoped live-location table and API endpoints in the PHP backend.
2. Android runtime location permission and foreground location updates while a ride session is active.
3. Periodic authenticated location uploads and polling (or WebSocket/Firebase) for other joined riders.
4. Rider markers on MapLibre, with stale-location timestamps and a stop-sharing control.
5. Authorization so only members of the same active ride can view each other's locations.

The existing public OSRM profile is `driving`; it is not a motorcycle-specific routing profile and does not provide a guarantee of motorcycle-safe roads or turn-by-turn voice guidance.
