# PostCraft

Native Android app (Kotlin + Jetpack Compose, no Expo): share a photo to it, get AI-restyled Instagram posts with captions, post with one tap.

## Flow
1. Google Photos / Gallery / Google app → **Share** → **PostCraft** (shared image links work too).
2. Pick aesthetic, post type, caption tone, format and number of variations → **Generate post**.
3. Pick your favourite variation, edit the caption → **Post to Instagram**.

## Setup on the phone
- Settings → paste a Gemini API key (https://aistudio.google.com/apikey). Image generation may need billing on that Google project.
- Optional **Direct publish** (posts with zero further taps): needs an Instagram Business/Creator account,
  Instagram user ID + long-lived access token from a Meta app with the Instagram API, and a free imgbb key
  (Instagram's API only accepts public image URLs; uploads expire after 1 hour).
  Without it, the button opens Instagram with the image attached and the caption on the clipboard.

## Build
```
./gradlew assembleRelease        # → app/build/outputs/apk/release/app-release.apk
```
Release signing reads `keystore.properties` + `postcraft-release.jks` (both git-ignored).
**Back up the keystore** — updates to an installed app must be signed with the same key.
