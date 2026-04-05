# AGENTS.md: Technical Guide for Snake-Droid Evolution

This guide is intended for AI agents, vibe coders, and developers looking to extend or maintain the KorGE + Physics based Snake game.

## 1. Architecture Overview
The game uses the **KorGE Game Engine** for the game loop and rendering. (Physics integration is being iterated for stability).

### Key Components:
- `Main.kt`: Contains the entry point and the main `GameScene`.
- `RobotSnake`: Consists of a head and segments with custom follow logic.

## 2. Technical Requirements & Limitations
- **KorGE Version**: 5.4.0
- **JVM Target**: 1.8. This is a critical requirement for Android/Kotlin Multiplatform compatibility in this project.
- **Dependency Management**: Standard Android/Kotlin plugins are managed to avoid conflicts with KorGE's multiplatform task generation.

## 3. Implementation Details

### 2.5D Coordinate System
- Visual depth is simulated by layering rectangles (Depth, Top, Detail).

### Movement Logic
- Movement is **Momentum-Based**. Custom velocity and friction are applied to the head.
- **Cable Following**: Segments follow the preceding part using distance-constrained vector math.

### Collisions
- Body identification is handled via `userData` (e.g., "wall", "battery", "crate").

## 4. Extending the Game

### Adding New Obstacles
1. Define a `dynamicBody` or `staticBody`.
2. Assign a `userData` string for identification.
3. Add a visual `View` (like a `solidRect` or `image`) and sync its position with the physics body in an `addUpdater` loop.

### Implementing Shaders/Filters
- Use `View.filter`. Note that some advanced filters like `DropShadowFilter` may have dependency or compatibility issues in specific multiplatform environments. Standard `BlurFilter` and `ColorMatrixFilter` are safe.

### Lighting
- For optimized lighting, simulate point lights using large, faint, scaling `Circle` views with low alpha.

## 5. Build & Verification
- Always verify changes with `./gradlew assembleDebug`.
- If dependency errors occur regarding `kbox2d`, ensure the `kbox2d-android` coordinate is used instead of the generic one.
