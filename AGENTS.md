# AGENTS.md: Technical Guide for Snake-Droid Evolution

This guide is intended for AI agents, vibe coders, and developers looking to extend or maintain the KorGE + Physics based Snake game.

## 1. Architecture Overview
The game uses the **KorGE Game Engine** for the game loop and rendering, and **KBox2D** (a port of Box2D) for physical interactions.

### Key Components:
- `Main.kt`: Contains the entry point and the main `GameScene`.
- `RobotSnake`: A physical entity consisting of a `dynamicBody` head and segments connected by `DistanceJoint`s.
- `KBox2D World`: A zero-gravity physics world where all movement and collisions occur.

## 2. Technical Requirements & Limitations
- **KorGE Version**: 5.4.0
- **KBox2D Version**: 3.0.0 (android-specific target used for dependency stability).
- **JVM Target**: 1.8. This is a critical requirement for Android/Kotlin Multiplatform compatibility in this project. Do not upgrade to 11/17/21 without verifying the multiplatform task configuration.
- **PTM_RATIO**: 20f. All physics units (meters) are scaled by 20 to convert to screen pixels.

## 3. Implementation Details

### 2.5D Coordinate System
- Visual depth is simulated by layering rectangles (Depth, Top, Detail).
- Z-Ordering: Moving objects should ideally set `zIndex = y` to handle overlapping correctly, though in KorGE 5.4.0, z-ordering in containers requires careful management of the display list.

### Movement Logic
- Movement is **Force-Based**. Instead of setting X/Y positions directly, apply linear impulses to the `headBody` of the snake.
- `linearDamping` is set to `2.0` on all segments to simulate friction and provide heavy momentum.

### Collisions
- Uses `ContactListener` from KBox2D.
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
