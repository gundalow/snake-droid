package dev.gundalow.snake

import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.kbox2d.*
import korlibs.korge.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlin.random.*

suspend fun main() =
    Korge(windowSize = Size(1280, 720), backgroundColor = Colors["#0a0a14"]) {
        sceneContainer().changeTo({ GameScene() })
    }

class MechanicalSnake(val scene: SContainer, val world: World, val gameScene: GameScene) {
    val PPM = gameScene.PPM
    val head: Body
    val tail: Body
    val segments = mutableListOf<Body>()
    val segmentViews = mutableMapOf<Body, View>()

    init {
        // Head
        head = world.createBody(
            BodyDef(
                type = BodyType.DYNAMIC,
                position = Vector2D(640 / PPM, 360 / PPM),
                linearDamping = 4.0f,
                angularDamping = 3.0f
            )
        )
        head.createFixture(FixtureDef(shape = BoxShape(32f / 2 / PPM, 32f / 2 / PPM), density = 5.0f))
        val headView = with(gameScene) { scene.createSnakeHead() }
        segmentViews[head] = headView

        // Tail
        tail = world.createBody(
            BodyDef(
                type = BodyType.DYNAMIC,
                position = Vector2D(640 / PPM, (360 + 20) / PPM), // 24-4=20 for 4px overlap
                linearDamping = 4.0f,
                angularDamping = 3.0f
            )
        )
        tail.createFixture(FixtureDef(shape = BoxShape(24f / 2 / PPM, 24f / 2 / PPM), density = 1.0f))
        val tailView = with(gameScene) { scene.createSnakeTail() }
        segmentViews[tail] = tailView

        // Joint between head and tail
        world.createJoint(
            RevoluteJointDef(
                bodyA = head,
                bodyB = tail,
                localAnchorA = Vector2D(0, 12 / PPM), // 16-4=12 for 4px overlap
                localAnchorB = Vector2D(0, -12 / PPM),
                enableLimit = true,
                lowerAngle = (-45).degrees,
                upperAngle = 45.degrees
            )
        )

        scene.addUpdater {
            updateViews()
        }
    }

    private fun updateViews() {
        for ((body, view) in segmentViews) {
            view.xy(body.position.x * PPM, body.position.y * PPM)
            view.rotation = body.angle.radians.radians
            view.zIndex = view.y
        }
    }

    fun applyForce(force: Vector2D) {
        head.applyForceToCenter(force * 100.0) // Scaling force for 5kg mass
    }

}

class GameScene : Scene() {
    val PPM = 30f

    fun SContainer.createIndustrialWall(rect: Rectangle, world: World): View = container {
        xy(rect.x, rect.y)
        // Physics body for wall
        val wallBody = world.createBody(
            BodyDef(
                type = BodyType.STATIC,
                position = Vector2D((rect.x + rect.width / 2) / PPM, (rect.y + rect.height / 2) / PPM)
            )
        )
        wallBody.createFixture(FixtureDef(shape = BoxShape(rect.width / 2 / PPM, rect.height / 2 / PPM)))
        wallBody.setUserData("wall")

        // Visuals
        solidRect(rect.width, rect.height, Colors["#111111"]).xy(0, 8)
        solidRect(rect.width, rect.height, Colors["#333333"])
        val b = 2.0
        solidRect(b, b, Colors.WHITE).xy(4, 4)
        solidRect(b, b, Colors.WHITE).xy(rect.width - 6, 4)
    }

    fun SContainer.createSnakeHead(): View = container {
        val gunmetal = Colors["#2A2C31"]
        val cyan = Colors["#00F0FF"]
        // Main head block
        roundRect(Size(32, 32), RectCorners(4f), fill = gunmetal)
        // Visor
        solidRect(24, 4, cyan).xy(4, 8)
        // Glowing effect (PointLight simulation)
        val light = circle(10.0, fill = cyan).xy(16, 10).alpha(0.3)
        light.addUpdater {
            this.scale = 1.0 + 0.2 * kotlin.math.sin(it.milliseconds / 100.0)
        }
    }

    fun SContainer.createSnakeBody(): View = container {
        val gunmetal = Colors["#2A2C31"]
        val yellow = Colors["#FFD700"]
        val black = Colors["#000000"]

        // Background
        solidRect(24, 24, gunmetal)

        // Caution stripes (simplified with a few diagonal lines)
        for (i in 0 until 4) {
            val offset = i * 8
            line(Point(offset, 0), Point(offset + 8, 24), yellow)
        }

        // Mechanical hinges (with 4px overlap)
        circle(4.0, fill = Colors.DARKGREY).xy(12, 0)
        circle(4.0, fill = Colors.DARKGREY).xy(12, 24)
    }

    fun SContainer.createSnakeTail(): View = container {
        val gunmetal = Colors["#2A2C31"]
        val red = Colors["#FF0033"]

        // Tapered tail using a polygon
        val points = listOf(
            Point(0, 0),
            Point(24, 0),
            Point(17, 24),
            Point(7, 24)
        )
        vectorPath {
            moveTo(points[0])
            lineTo(points[1])
            lineTo(points[2])
            lineTo(points[3])
            close()
        }.fill(gunmetal)

        // Red LED
        val led = circle(3.0, fill = red).xy(12, 18)
        led.addUpdater {
            val alpha = 0.6 + 0.4 * kotlin.math.sin(it.milliseconds / 200.0)
            this.alpha = alpha
        }
    }

    fun SContainer.createFuelCell(): View = container {
        val darkGrey = Colors["#333333"]
        val neonGreen = Colors["#39FF14"]

        roundRect(Size(20, 28), RectCorners(2f), fill = darkGrey)
        solidRect(12, 20, neonGreen).xy(4, 4).alpha(0.8)
    }

    override suspend fun SContainer.sceneMain() {
        // DropShadowFilter is available in KorGE, but sometimes it might be in korlibs-image or korlibs-korge-view
        // We'll use a container with a filter if possible, or just skip if it causes issues.
        // For now, let's assume it works.
        // this.filter = korlibs.korge.view.filter.DropShadowFilter()

        val world = World(Vector2D(0, 0))

        // Setup contact listener for sparks
        world.setContactListener(object : ContactListener {
            override fun beginContact(contact: Contact) {
                val bodyA = contact.fixtureA.body
                val bodyB = contact.fixtureB.body
                if (bodyA.userData == "wall" || bodyB.userData == "wall") {
                    // Trigger sparks at collision point
                    val worldManifold = WorldManifold()
                    contact.getWorldManifold(worldManifold)
                    val point = worldManifold.points[0]
                    createSparks(point * PPM)
                }
            }
            override fun endContact(contact: Contact) {}
            override fun preSolve(contact: Contact, oldManifold: Manifold) {}
            override fun postSolve(contact: Contact, impulse: ContactImpulse) {}
        })

        // Background
        val bgBitmap = Bitmap32(64, 64) { x, y ->
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

        // Walls
        val width = 1280.0
        val height = 720.0
        val t = 32.0
        createIndustrialWall(Rectangle(0.0, 0.0, width, t), world)
        createIndustrialWall(Rectangle(0.0, height - t, width, t), world)
        createIndustrialWall(Rectangle(0.0, t, t, height - 2 * t), world)
        createIndustrialWall(Rectangle(width - t, t, t, height - 2 * t), world)

        val snake = MechanicalSnake(this, world, this@GameScene)

        // Fuel Cell (Food)
        var fuelCell = createFuelCell()
        fuelCell.xy(800, 360)

        fun relocateFuelCell() {
            fuelCell.xy(Random.nextDouble(100.0, 1100.0), Random.nextDouble(100.0, 600.0))
        }

        var dragStart = Point(0, 0)
        onDown { dragStart = it.lastPosGlobal }
        onUp {
            val dragEnd = it.lastPosGlobal
            val delta = dragEnd - dragStart
            if (delta.length > 20.0) {
                val forceDir = delta.normalized
                snake.applyForce(forceDir.toVector())
            }
        }

        addUpdater {
            world.step(it.seconds.toFloat(), 8, 3)

            // Simple collision check for food
            val head = snake.head
            val headPos = head.position * PPM
            if (Point.distance(headPos, fuelCell.pos) < 30.0) {
                relocateFuelCell()
                addBodySegment(world, snake)
            }
        }
    }

    fun SContainer.addBodySegment(world: World, snake: MechanicalSnake) {
        val parent = if (snake.segments.isEmpty()) snake.head else snake.segments.last()

        val tailJoint = world.jointList.find { (it.bodyA == parent && it.bodyB == snake.tail) || (it.bodyA == snake.tail && it.bodyB == parent) }
        if (tailJoint != null) {
            world.destroyJoint(tailJoint)
        }

        val pos = (parent.position + snake.tail.position) * 0.5f
        val newSegment = world.createBody(
            BodyDef(
                type = BodyType.DYNAMIC,
                position = pos,
                linearDamping = 4.0f,
                angularDamping = 3.0f,
                isActive = false
            )
        )
        newSegment.createFixture(FixtureDef(shape = BoxShape(24f / 2 / PPM, 24f / 2 / PPM), density = 1.0f))
        val view = createSnakeBody()
        snake.segmentViews[newSegment] = view
        snake.segments.add(newSegment)

        world.createJoint(
            RevoluteJointDef(
                bodyA = parent,
                bodyB = newSegment,
                localAnchorA = if (parent == snake.head) Vector2D(0, 12 / PPM) else Vector2D(0, 12 / PPM),
                localAnchorB = Vector2D(0, -12 / PPM),
                enableLimit = true,
                lowerAngle = (-45).degrees,
                upperAngle = 45.degrees
            )
        )

        world.createJoint(
            RevoluteJointDef(
                bodyA = newSegment,
                bodyB = snake.tail,
                localAnchorA = Vector2D(0, 12 / PPM),
                localAnchorB = Vector2D(0, -12 / PPM),
                enableLimit = true,
                lowerAngle = (-45).degrees,
                upperAngle = 45.degrees
            )
        )

        launchImmediately {
            delay(500.milliseconds)
            newSegment.isActive = true
        }
    }
}
