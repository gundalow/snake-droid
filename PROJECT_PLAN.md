# PROJECT_PLAN.md: Snake-Droid Evolution (KorGE + Physics)

This document tracks the migration and evolution of the Snake game from Jetpack Compose to the KorGE Game Engine with physical interactions.

## 1. Engine & Environment (Migration)
- [X] **KorGE Foundation**: Migrated core game loop to KorGE `Scene` architecture.
- [X] **Gradle Configuration**: Integrated KorGE 5.4.0 and KBox2D plugins.
- [X] **JVM Toolchain**: Resolved version mismatches and multiplatform conflicts.
- [X] **Industrial World**: Created a 2.5D 'Industrial' level with metallic tiled floor and layered walls.

## 2. Physics-Based Mechanics (KBox2D)
- [X] **World Initialization**: Box2D world with zero gravity.
- [X] **Mechanical Snake**:
    - [X] Physical head (CircleShape with high density).
    - [X] Segmented cable body (10 segments) connected via `DistanceJoint`s.
    - [X] Organic dragging weight using `linearDamping = 2.0`.
- [X] **Force-Based Control**: Snake movement handled by applying impulses/forces to the head.
- [X] **Destructible Obstacles**:
    - [X] Wooden crates with dynamic physics.
    - [X] High-velocity impact detection that shatters crates into splinters.

## 3. Graphics & "Juice"
- [X] **2.5D Perspective**: Simulated depth for walls and crates.
- [X] **Collision Feedback**:
    - [X] Orange spark particles on wall impacts.
    - [X] Momentary scale/tint feedback on snake head damage.
- [X] **Energy Battery (Food)**:
    - [X] Spawning logic for Cyan batteries.
    - [X] "Energy ripple" effect upon collection.

## 4. Future Work
- [ ] **Real-time Lighting**: Implement PointLight for head/batteries and Navy Blue ambient light.
- [ ] **Shadows**: Add DropShadowFilter or simulated shadows for all dynamic objects.
- [ ] **Audio Integration**: Load and play 'eat.wav' and 'hit.wav' during events.
- [ ] **Procedural Skin**: Implement a smooth hull/mesh around physical segments.
- [ ] **Achievement System**: Port the milestone system to the new engine.

## 5. CI & Testing
- [X] **Build Verification**: Project compiles and builds Android APK (`assembleDebug`).
- [ ] **Automated Tests**: Port unit tests to verify physics interactions.
