package dev.gundalow.snake

import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.math.*

suspend fun main() =
    Korge(windowSize = Size(1280, 720), backgroundColor = Colors["#0a0a14"]) {
        sceneContainer().changeTo({ GameScene() })
    }

class RobotSnake(val container: Container) {
    val head: View
    val segments = mutableListOf<View>()
    private var velocity = Vector2D(0, 0)
    private val acceleration = 2.0
    private val friction = 0.95
    private val segmentDistance = 20.0

    init {
        head = container.circle(16.0, fill = Colors.DARKSLATEGRAY).xy(640, 360)
        for (i in 1..10) {
            val seg = container.circle(12.0, fill = Colors.DARKGREY).xy(640, 360 + i * 20)
            segments.add(seg)
        }

        container.addUpdater {
            velocity *= friction
            head.xy(head.x + velocity.x, head.y + velocity.y)

            var prevPos = head.pos
            for (seg in segments) {
                val dx = seg.x - prevPos.x
                val dy = seg.y - prevPos.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > segmentDistance) {
                    val angle = atan2(dy, dx)
                    seg.xy(
                        prevPos.x + cos(angle) * segmentDistance,
                        prevPos.y + sin(angle) * segmentDistance,
                    )
                }
                prevPos = seg.pos
            }
        }
    }

    fun applyImpulse(dir: Vector2D) {
        velocity += dir * acceleration
    }
}

class GameScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bgBitmap =
            Bitmap32(64, 64) { x, y ->
                val v = (x + y) % 2 * 5 + 15
                Colors["#0a0a14"].withR(v).withG(v).withB(v)
            }
        container {
            for (x in 0 until 20) {
                for (y in 0 until 12) {
                    image(bgBitmap).xy(x * 64, y * 64)
                }
            }
        }

        val width = 1280.0
        val height = 720.0
        val t = 32.0
        createIndustrialWall(Rectangle(0.0, 0.0, width, t))
        createIndustrialWall(Rectangle(0.0, height - t, width, t))
        createIndustrialWall(Rectangle(0.0, t, t, height - 2 * t))
        createIndustrialWall(Rectangle(width - t, t, t, height - 2 * t))

        val snake = RobotSnake(this)

        keys {
            down(Key.UP) { snake.applyImpulse(Vector2D(0, -1)) }
            down(Key.DOWN) { snake.applyImpulse(Vector2D(0, 1)) }
            down(Key.LEFT) { snake.applyImpulse(Vector2D(-1, 0)) }
            down(Key.RIGHT) { snake.applyImpulse(Vector2D(1, 0)) }
        }
    }

    fun SContainer.createIndustrialWall(rect: Rectangle) {
        container {
            xy(rect.x, rect.y)
            solidRect(rect.width, rect.height, Colors["#111111"]).xy(0, 8)
            solidRect(rect.width, rect.height, Colors["#333333"])
            val b = 2.0
            solidRect(b, b, Colors.WHITE).xy(4, 4)
            solidRect(b, b, Colors.WHITE).xy(rect.width - 6, 4)
        }
    }
}
