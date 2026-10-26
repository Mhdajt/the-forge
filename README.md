# The Forge

Minecraft Fabric mod (Minecraft 26.2, Java 25) that plays full-screen synchronized videos with audio.

## Commands

- `/video <name> [player]` — e.g. `/video the_forge @s` or `/video the_forge_tiny @a`
- `/video stop [player]` — Stop current video playback
- `/forgevideo stretch | cover | fit` — Client display fit mode (default: `stretch`)

## Video Files

Video files (`.fgv`) are stored on each player's computer at:
```
<game folder>/the_forge/<name>.fgv
```

Supported video names:
- `the_forge` (Full 1080p, 1x normal speed synchronized with the 57s audio)
- `the_forge_tiny` (720p lightweight version)

To convert your own videos to `.fgv`:
```bash
python tools/make_video.py input.mp4 30
```

Audio is configured in `src/main/resources/assets/the_forge/sounds.json` and bundled in `sounds/video.ogg`.

## Building

This project uses Gradle and GitHub Actions:
- Push to GitHub triggers `.github/workflows/build.yml`
- Download the built `.jar` from GitHub Actions **Artifacts** tab (`the-forge-video-mod`).
