package com.example.app1

import com.example.app1.game.Bullet
import com.example.app1.game.Direction
import com.example.app1.game.GameEngine
import com.example.app1.game.GameStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    @Test
    fun startingLevelSpawnsEnemyImmediately() {
        val engine = GameEngine()

        engine.start(0)

        assertTrue(engine.state.enemies.isNotEmpty())
    }

    @Test
    fun quickDirectionTapMovesPlayerBeforeNextFrame() {
        val engine = GameEngine()
        engine.start(0)
        val startY = engine.state.player.y

        engine.setDirection(Direction.Up)
        engine.setDirection(null)
        engine.update(16L)

        assertTrue(engine.state.player.y < startY)
    }

    @Test
    fun firingAddsBulletImmediately() {
        val engine = GameEngine()
        engine.start(0)
        val bullets = engine.state.bullets.size

        engine.firePlayer()

        assertEquals(bullets + 1, engine.state.bullets.size)
    }

    @Test
    fun enemyBulletHitResetsPlayerWithoutConcurrentModification() {
        val engine = GameEngine()
        engine.start(0)
        val player = engine.state.player
        engine.state.bullets += Bullet(
            x = player.x + 0.37f,
            y = player.y + 0.37f,
            direction = Direction.Up,
            fromPlayer = false,
            speed = 0f
        )

        engine.update(16L)

        assertEquals(GameStatus.Playing, engine.state.status)
        assertEquals(2, engine.state.player.lives)
        assertTrue(engine.state.bullets.none { !it.fromPlayer })
    }

    @Test
    fun holdingDirectionMovesPlayer() {
        val engine = GameEngine()
        engine.start(0)
        val startY = engine.state.player.y

        engine.setDirection(Direction.Up)
        engine.update(16L)

        assertTrue(engine.state.player.y < startY)
        assertEquals(Direction.Up, engine.state.player.direction)
    }

    @Test
    fun releasingDirectionStopsPlayerMovement() {
        val engine = GameEngine()
        engine.start(0)

        engine.setDirection(Direction.Up)
        engine.update(16L)
        val movedY = engine.state.player.y
        engine.setDirection(null)
        engine.update(16L)

        assertEquals(movedY, engine.state.player.y)
    }
}
