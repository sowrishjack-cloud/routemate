# RouteMate 🏍️

**RouteMate** is a modern Android application built with **Kotlin** and **Jetpack Compose**, designed for motorcycle enthusiasts, road trippers, and touring groups to find riding partners, discover scenic destinations, coordinate team rides, and share highway adventures.

---

## ✨ Features

- **Story-Driven Hero Carousel:**
  - High-resolution, travel-focused carousel highlighting rider community, mountain routes, team rides, rider mentorship, and shared road memories.
  - Smooth continuous looping auto-scroll with synchronized animated indicator dots.
- **Interactive One-Drag Screen Navigation:**
  - Real-time, finger-following horizontal page switching between the 5 primary tabs:
    1. **Home** — Featured routes, quick actions, popular destinations, budget-friendly trips, and nearby riders.
    2. **Match** — Filter riders by destination, budget slider (₹0 - ₹40,000+), and riding experience level.
    3. **Teams** — Discover motorcycle crews with vacant seats, route itineraries, and team briefings.
    4. **Learn** — Highway safety checklists, mountain road guidance, maintenance guides, and road wisdom.
    5. **Profile** — Rider profile management with custom photo picker, emergency contacts, bike details, and ride stats.
  - Clamped swipe gestures with single-screen advancement and natural settling physics.
- **Synchronized Bottom Navigation Bar:**
  - Custom floating navigation bar with active cyan pill indicators tracking interactive drags in real time.
  - Direct tab tapping with instant smooth animation.
- **Top-Right Quick Actions Capsule:**
  - **Settings (⚙️):** Toggle dynamic Dark/Light theme, granular push notification controls (ride invites, highway weather alerts, team broadcasts), and GPS location services with live satellite telemetry.
  - **Route Map (🗺️):** Live road previews and highway traffic alerts.
  - **Notifications (🔔):** Live ride alerts and invitation management with real-time in-app alert banner.
  - **Navigation (🧭):** Active GPS navigation telemetry and next turn maneuvers.
- **Trip Details & Coordination:**
  - Deep-dive into route checkpoints, distance, duration, estimated budget, and meetup points.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Image Loading:** Coil Compose
- **Architecture:** Clean Architecture with MVVM pattern
- **State Management:** Reactive Compose State & StateFlow
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 14 / 15 (API 34/35)
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalogs (`libs.versions.toml`)

---

## 🚀 Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/sowrishjack-cloud/routemate.git
   cd routemate
   ```

2. **Open in Android Studio:**
   - Open Android Studio (Ladybug or newer recommended).
   - Select **Open** and choose the `RouteMate` project directory.
   - Wait for Gradle sync to complete.

3. **Run on Device / Emulator:**
   - Connect an Android device with USB debugging enabled or launch an Android Virtual Device (AVD).
   - Press **Run ▶** (Shift + F10) or build via CLI:
     ```bash
     ./gradlew assembleDebug
     ./gradlew installDebug
     ```

---

## 📄 License

This project is developed for motorcycle road trip and rider community networking.
