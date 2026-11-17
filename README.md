# Glassify

A local-only music player for Android 13+ with an adjustable liquid-glass interface.

- Plays the music already on your phone (MediaStore). No internet, no accounts.
- Songs / Albums / Artists, search, shuffle, repeat, background playback with notification.
- **Glass tab**: one "How glassy" slider (Solid → Frosted → Liquid → Crystal) plus fine-tune
  sliders for blur, refraction, tint opacity and edge shine. A live preview updates as you drag.
- Glass uses `RenderEffect` blur and an AGSL lens shader (needs Android 13, which is why minSdk = 33).

## Get the APK (no installs needed)

1. Create a free account at github.com and make a **new repository** (public is simplest).
2. Upload everything from this folder into it (**Add file → Upload files**, then drag the files in).
   Make sure the hidden `.github` folder is included. If it was skipped, use
   **Add file → Create new file**, type `.github/workflows/build.yml` as the name, and paste in
   the contents of that file from this project.
3. Open the **Actions** tab. The "Build APK" workflow starts by itself after the upload
   (or press **Run workflow**). It takes about 4–8 minutes.
4. Click the finished run (green tick) → scroll to **Artifacts** → download `Glassify-debug-apk`.
5. Unzip it, copy `app-debug.apk` to your phone, tap it, and allow "Install unknown apps" when asked.
6. Open Glassify and allow access to music (and notifications).

If the run fails (red cross), open it, copy the error from the failed step, and send it to Claude.

## Project layout

- `app/src/main/java/com/glassify/music/ui/glass/Glass.kt` — the glass engine (backdrop capture, blur, lens shader, rim light)
- `ui/` — screens (library, now playing, glass settings)
- `playback/` — Media3 service and controller
- `data/` — MediaStore scanning and saved settings
