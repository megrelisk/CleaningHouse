# CleanQuest 🧹⚔️ (Gamified AI house cleaning for two)

An Android app that turns house cleaning into a co-op game for two people.

## Features
- **Projects**: separate workspaces (e.g. "Main apartment", "Summer house"), each with its own quest history, levels, coins and reward store.
- **In-app camera**: record a video (up to 60 s) or take photos inside the app. No gallery upload.
- **AI quest generation** (Google Gemini): the AI looks at the video or photos and makes short task cards with the time each takes, for example "Wipe the glass coffee table · ⏱ 2 min · ⭐ 18 XP". You can choose Auto (5–50 tasks depending on the mess), or exactly 5 / 10 / 20 / 50.
- **🎲 Random run**: pick 1 / 3 / 5 / 10 random quests for a quick session.
- **Two animated characters** (boy and girl): when you finish a quest, choose who did it, or **Together** (75% each, so the team gets +50%).
- **XP, levels and titles** for each player, plus a shared house level. A confetti celebration plays on each completed quest and on level-ups.
- **Reward store**: create your own perks ("Buy me a Coca-Cola 🥤", "10-min foot massage 💆", …) and buy them with coins. Perks the other player still owes are tracked until they're delivered.
- Georgian 🇬🇪 / English UI. The AI quest language is set separately.

## Getting the APK
Every push runs **GitHub Actions → Build APK**:
- The **Releases** page has a `CleanQuest build N` pre-release with the `.apk` attached. Open it on your phone, download the file and install it. You may need to allow "Install unknown apps" for your browser.
- You can also download the APK from the workflow run's *Artifacts*.

Every build is signed with the same key (`app/cleanquest.keystore`), so a new build installs over the old one and keeps your progress.

## AI setup
1. Get a free key at https://aistudio.google.com/apikey
2. In the app, go to ⚙️ Settings → paste the **Gemini API key** (default model: `gemini-2.5-flash`).

Without a key, the app still works in **demo mode** with random sample quests.

Short videos (under about 9 MB) are sent to Gemini as video. Longer ones are reduced to 14 still frames, and photos are downscaled before upload.

## Build locally
Requires the Android SDK (API 35) and JDK 17+:
```
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

## Tech
Kotlin · Jetpack Compose (Material 3) · CameraX · Gemini REST API with JSON schema output · kotlinx.serialization (local JSON storage).
