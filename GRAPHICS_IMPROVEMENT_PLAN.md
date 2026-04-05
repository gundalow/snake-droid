# Graphics Improvement Plan: Retro-Arcade Vibe

This document outlines the detailed plan for transforming the Snake game from basic geometric placeholders to a high-fidelity "Retro Arcade" aesthetic using Jetpack Compose ("Vibe Coding").

## 1. Aesthetic Vision: "Cyber-Neon"
- **Colors**: High-contrast neon (Electric Green, Cyber Cyan, Hot Pink, Laser Red) against a deep dark background (#0A0A0A).
- **Effects**: Heavy use of gradients, glows (blur masks), and scanlines to mimic CRT monitors.
- **Depth**: Faux-3D achieved through layer stacking and dynamic drop shadows.

## 2. Component Breakdown

### 2.1 The Snake (Primary Focus)
*   **Design**: A tech-organic hybrid. Each segment is a "Voxel" or "Module".
*   **Faux-3D Implementation**:
    *   **Base**: Darker shade offset by 2dp (Shadow).
    *   **Body**: Primary neon color with a `RadialGradient` (center highlight).
    *   **Top**: A thin, bright "rim light" line on the leading edge.
*   **Vibe Additions**:
    *   **Glow**: Use `drawIntoCanvas` with `Paint().apply { maskFilter = BlurMaskFilter(...) }` to create a soft bloom around the head.
    *   **Trailing**: Body segments slightly smaller than the head to create a tapering effect.
    *   **Eyes**: Two bright white dots on the head that always face the `currentDirection`.

### 2.2 Food & Mega-Melon
*   **Design**: Stylized vector fruits with neon outlines.
*   **Animations**:
    *   **Levitation**: Vertical offset `y = sin(time * speed) * amplitude` to make food "float" above the grid.
    *   **Shadow**: A small dark ellipse on the grid that scales (shrinks/grows) as the food floats up/down.
*   **Mega-Melon Special**:
    *   A pulsing magenta aura.
    *   Internal "seeds" that glow periodically.
    *   When bitten, emit "juice" particles (short-lived neon circles).

### 2.3 Hazards: UFO & World-Stomper
*   **UFO (Galactic Greed)**:
    *   **Body**: Three stacked ellipses (Base, Middle, Dome) to create a saucer shape.
    *   **Lights**: Rotating colored dots along the rim.
    *   **Tractor Beam**: A `Brush.verticalGradient` (Cyan -> Transparent) from the UFO to the target food. Add a `graphicsLayer` alpha pulse.
*   **World-Stomper (Tectonic Tussle)**:
    *   **Foot**: A large "Robot Foot" with visible bolts and hydraulic lines (Vector asset).
    *   **Descent**: Scaling effect + Shadow opacity increase.
    *   **Impact**: Create a "Shockwave" circle that expands from (0,0) using `Stroke` with decreasing alpha.

### 2.4 Environment & UI
*   **The Grid**:
    *   Thin, dark-blue lines. Every 5th line is slightly brighter.
    *   Perspective warp: Draw the grid with a slight vertical skew to enhance the 3D feel.
*   **Post-Processing**:
    *   **Scanlines**: A full-screen overlay of semi-transparent horizontal lines.
    *   **Vignette**: A `RadialGradient` from transparent center to black edges.
*   **UI Overlay**:
    *   Use "Digital" or "Monospace" fonts.
    *   "Glitch" effect on the "GAME OVER" text (random horizontal offsets for a few frames).

## 3. Implementation Strategy (Vibe Coding)

### Phase A: Snake & Food (The Core)
1.  Replace `drawRect` in `drawSnake` with a custom `drawSnakeSegment` function that implements the stacked-layer logic.
2.  Update `drawFood` to include the sine-wave levitation and ground shadow.
3.  Add "Spawn" and "Eat" particle effects (simple Canvas-based particle system).

### Phase B: Hazards & Polish
1.  Redraw the UFO using layered ellipses and a gradient tractor beam.
2.  Enhance the Stomper with a detailed vector graphic and impact shockwaves.
3.  Implement the "Screen Shake" with more intensity and frequency.

### Phase C: Screen-Space Effects
1.  Apply the scanline and vignette overlays to the `GameCanvas`.
2.  Add a "CRT Flicker" effect (subtle alpha oscillation of the entire canvas).

## 4. Technical Considerations
*   **Performance**: Use `graphicsLayer` for any full-screen animations (like flicker or scanlines) to keep them on the GPU.
*   **Scalability**: All graphics will be drawn relative to the `boardScale` to ensure they look sharp on all Android screen sizes.
*   **Asset Generation**:
    *   Generate `.xml` VectorDrawables for complex shapes (UFO dome, Stomper foot).
    *   Use `Path` API for procedural shapes (Snake segments).
