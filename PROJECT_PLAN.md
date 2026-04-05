# PROJECT_PLAN.md: 3D Snake (Implemented as 2D Top-Down)

This document tracks the implementation of the 3D Snake game mechanics into an Android Jetpack Compose application.

## 1. Game World
- [X] **Grid System**: The game operates on a 1.0-unit grid. Both the snake's movement and food placement are snapped to this grid.
- [X] **Board**: A flat 2D plane (the garden) surrounded by boundaries (fences).
- [X] **Coordinate System**:
  - **Playable Area**: Centered at (0, 0).
  - **Boundaries**: Walls are placed at a distance of 15.5 units from the center (total 31x31 area).
  - **View**: Top-down 2D.

## 2. Snake Mechanics
- [X] **Movement**:
  - [X] Constant Forward: The snake always moves forward at a constant base speed.
  - [X] Snap-Turning: Direction changes are requested by the player but only applied when the snake head reaches a grid boundary (every 1.0 unit).
  - [X] 180-Degree Reversal Prevention: The snake cannot immediately turn 180 degrees.
- [X] **Growth and Body Segments**:
  - [X] History-Following System: The snake's body consists of segments that follow the exact path taken by the head.
  - [X] High-resolution history buffer: Every 0.1 units traveled.
  - [X] Segment spacing: 1.0 unit (every 10 history points).
  - [X] Adding Segments: Eating food adds a new segment to the tail.
- [X] **Speed Progression**:
  - [X] Initial Speed: 5.0 units/sec.
  - [X] Incremental Increase: 0.2 units/sec per food eaten.

## 3. Gameplay Features
- [X] **Food and Items**:
  - [X] Normal Food: Spawns at random valid grid positions (not overlapping with snake).
  - [X] Effect: Increases score by 1, adds 1 segment, and increases speed.
- [X] **Mega-Melon**:
  - [X] Multi-Bite: Requires 3 bites to be fully consumed. Each bite adds segment and score.
  - [X] Scale Progression: Model scales down after each bite.
  - [X] Slow-Down Effect: Speed reduced by 50% during consumption.
  - [X] Burp Delay: 0.5s delay and "burp" sound after final bite.
- [X] **Hazards and Events**:
  - [X] Galactic Greed (UFO): Appears every 30s, targets food, abducts it. Penalty: -5 points.
  - [X] Tectonic Tussle (World-Stomper): giant foot appears every 30s, causes screen shake and relocates food.

## 4. Meta-Systems
- [X] **Achievement System**: triggered based on score milestones or food counts (e.g. 10, 20, 30, 50 apples).
- [X] **Leaderboard and Persistence**:
  - [X] Name Selection: Players can enter/select name at start.
  - [X] Data Storage: High scores stored in JSON file.
  - [X] Display: Top 10 scores on game over screen.

## 5. Death Conditions
- [X] **Wall Collision**: The game ends if the snake's head collides with the boundary walls.
- [X] **Self-Collision**: The game ends if the snake's head collides with any of its own body segments.
- [X] **Invulnerability**: The snake has a 0.5s window of invulnerability at the start.

## 6. Controls (Android Adaptation)
- [X] **Swipe Gestures**: North, South, East, West directional control via on-screen swipes.
- [X] **Restart**: Button on the Game Over screen.

## 7. Technical Constants
| Constant | Value | Status |
| :--- | :--- | :--- |
| `HISTORY_RESOLUTION` | 0.1 | [X] |
| `SEGMENT_SPACING` | 10 | [X] |
| `GRID_SIZE` | 1.0 | [X] |
| `INITIAL_MOVE_SPEED` | 5.0 | [X] |
| `SPEED_INCREMENT` | 0.2 | [X] |
| `INVULNERABILITY_TIME`| 0.5s | [X] |
| `BOARD_SIZE` | 31.0 | [X] |
| `WALL_DISTANCE` | 15.5 | [X] |

## 8. Assets
- [X] **Audio**: `whoosh.wav`, `apple.ogg`, `chew.ogg`, `burp1-3.ogg`, `impact.wav`, `tractor_beam.wav`.
- [X] **Placeholders**: 2D Circles/Rectangles for snake and food.

## 9. CI & Testing
- [X] **Unit Tests**:
  - `SnakeGameEngineTest`: Verifies movement logic, grid-snapping, food consumption, and collision detection.
- [X] **Static Analysis**:
  - Integration of `ktlint` for Kotlin code style.
  - Integration of `detekt` for static analysis.
- [X] **UI Testing**:
  - `SnakeGameUiTest`: Basic Compose UI tests to verify name entry and game initialization.

## 10. Phase 2: Retro Arcade Visual Overhaul (PLANNED)
- [ ] **Aesthetic**: "Cyber-Retro" neon arcade vibe. Dark background with vibrant glowing elements.
- [ ] **Perspective**: Faux-3D using stacking layers and drop shadows to simulate depth.
- [ ] **Snake Overhaul**:
    - Replace basic squares with neon-bordered rounded rects or custom SVGs.
    - Add "tail-tapering" and "head-glow".
    - Segment connection lines with "pulse" effect.
- [ ] **Food Overhaul**:
    - Sprite-based or complex vector food (Apples/Melons) with neon highlights.
    - "Floating" animation (sine-wave vertical movement).
- [ ] **Environment**:
    - Grid lines with perspective/scanline effects.
    - CRT-flicker or subtle noise overlay.
- [ ] **Hazards**:
    - UFO: Saucer with rotating lights and tractor beam gradient.
    - Stomper: More detailed "Robot Foot" with mechanical details and ground impact debris particles.
