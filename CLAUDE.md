# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.example.resqnet.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Install on connected device
./gradlew installDebug
```

## Architecture Overview

ResQNet is an Android emergency-response app built with **Jetpack Compose + MVVM + Repository pattern**, targeting API 24+, compiled against API 36.

### User Roles & Navigation Flow

Three roles drive the entire navigation tree (see `NavGraph.kt`):
- **REQUESTER** → `HomeScreen` → can trigger SOS → `SOSTriggerSheet` → `SOSActiveScreen`
- **VOLUNTEER** → `VolunteerHomeScreen` → `IncomingSosScreen` → `ActiveResponseScreen`
- **ADMIN** → `AdminDashboardScreen`

After login/register, `roleToRoute(role)` maps the role string to its home screen. The shared `AuthViewModel` (scoped to `NavHost`) calls `checkAuthState` on app start to skip onboarding if already authenticated.

### Layer Structure

```
data/repository/   → interfaces (AuthRepository, SosRepository, VolunteerRepository, UserRepository, AdminRepository)
data/firebase/     → Firebase implementations (real backend, used in production)
data/mock/         → In-memory mock implementations + FakeData singleton (for development/UI testing)
domain/model/      → pure data classes: User, SosRequest, SosResponse, Feedback
ui/screens/<role>/ → Screen composables + co-located ViewModel per feature area
ui/components/     → shared composables (SOSButton, StatusBadge, VolunteerCard, etc.)
navigation/        → NavGraph.kt (single NavHost) + Routes.kt (sealed class)
util/              → Constants.kt, LocationUtil.kt
```

### Key Patterns

**ViewModels default to Firebase repositories** — e.g. `SosViewModel(private val sosRepository: SosRepository = FirebaseSosRepository())`. To swap in mocks, pass the mock in the constructor (no DI framework is used).

**AuthViewModel needs a Factory** because `FirebaseAuthRepository` requires `Context` for SharedPreferences (7-day session timer stored in `resqnet_auth_prefs`). All other ViewModels use the default no-arg `viewModel()` call.

**Repository methods return `Result<T>` and are `suspend fun`**. Callers use `.fold(onSuccess, onFailure)` or `.onSuccess {}`.

**Real-time SOS status** uses `callbackFlow` wrapping a Firestore snapshot listener (`observeSosStatus` in `FirebaseSosRepository`). The `SosViewModel` collects this flow in a long-lived coroutine.

**Routes with parameters** use a companion `createRoute(id)` function on the sealed object, e.g. `Routes.SosActive.createRoute(sosId)`.

### Firebase Collections

| Collection | Purpose |
|---|---|
| `users` | User profiles including `role`, `volunteerProfile` sub-map |
| `sos_requests` | SOS documents; `location` field is a Firestore `GeoPoint` |

The `PENDING → NOTIFIED` transition is currently simulated client-side in `SosViewModel.loadActiveSos()` with a 4-second delay calling `advanceToNotified()`. In production this should be a Cloud Function.

### Location Strategy

`LocationUtil.getCurrentLocation(context)` uses a two-step approach: last-known location first (fast), then high-accuracy FusedLocationProvider, with Delhi coordinates as fallback. Map rendering uses OSMDroid.
