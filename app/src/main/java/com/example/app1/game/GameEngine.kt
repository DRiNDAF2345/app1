package com.example.app1.game

import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.math.floor
import kotlin.random.Random

class GameEngine(private val random: Random = Random.Default) {
    private var tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }.getOrNull()
    var state: GameState = createState(0)
        private set
    private var version: Int = 0
    fun version(): Int = version

    fun start(levelIndex: Int) {
        state = createState(levelIndex.coerceIn(0, Levels.all.lastIndex))
        version++
    }

    fun setDirection(direction: Direction?) {
        state.inputDirection = direction
    }

    fun firePlayer() {
        val player = state.player
        if (player.reloadMs <= 0L && state.status == GameStatus.Playing) {
            addBullet(player, true)
            player.reloadMs = 380L
            beep(ToneGenerator.TONE_PROP_BEEP, 45)
            version++
        }
    }

    fun update(deltaMs: Long) {
        if (state.status != GameStatus.Playing) return
        val dt = deltaMs.coerceAtMost(48L)
        state.player.reloadMs = (state.player.reloadMs - dt).coerceAtLeast(0L)
        movePlayer()
        spawnEnemies(dt)
        updateEnemies(dt)
        updateBullets()
        updateExplosions(dt)
        if (state.remainingEnemies <= 0) state.status = GameStatus.LevelWon
        version++
    }

    private fun createState(levelIndex: Int): GameState {
        val level = Levels.all[levelIndex]
        val tiles = MutableList(level.height) { y ->
            MutableList(level.width) { x ->
                when (level.map[y][x]) {
                    'B' -> TileType.Brick
                    'M' -> TileType.Metal
                    'H' -> TileType.Base
                    else -> TileType.Empty
                }
            }
        }
        var playerStart = 1 to (level.height - 2)
        val enemySpawns = mutableListOf<Pair<Int, Int>>()
        var base = level.width / 2 to (level.height - 1)
        level.map.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                when (c) {
                    'P' -> playerStart = x to y
                    'E' -> enemySpawns += x to y
                    'H' -> base = x to y
                }
            }
        }
        val player = Tank(playerStart.first + .13f, playerStart.second + .13f, Direction.Up, 2.25f, lives = 3, isPlayer = true)
        return GameState(levelIndex, tiles = tiles, player = player, enemySpawns = enemySpawns, playerStart = playerStart, baseCell = base, enemiesLeftToSpawn = level.enemyCount)
    }

    private fun movePlayer() {
        val dir = state.inputDirection ?: return
        state.player.direction = dir
        tryMoveTank(state.player, dir, state.player.speed / 60f)
    }

    private fun spawnEnemies(deltaMs: Long) {
        val level = Levels.all[state.levelIndex]
        state.spawnTimerMs -= deltaMs
        if (state.enemiesLeftToSpawn <= 0 || state.spawnTimerMs > 0L || state.enemies.size >= 4) return
        val freeSpawns = state.enemySpawns.filter { (x, y) -> canOccupy(x + .13f, y + .13f, ignore = null) }
        if (freeSpawns.isNotEmpty()) {
            val cell = freeSpawns[random.nextInt(freeSpawns.size)]
            val enemy = Enemy(cell.first + .13f, cell.second + .13f, Direction.Down, level.enemySpeed)
            state.enemies += enemy
            state.enemyPlans[enemy] = EnemyPlan(changeDirMs = random.nextLong(500L, 1400L), shootMs = random.nextLong(450L, level.enemyShootDelayMs + 600L))
            state.enemiesLeftToSpawn--
        }
        state.spawnTimerMs = level.enemySpawnDelayMs
    }

    private fun updateEnemies(deltaMs: Long) {
        val level = Levels.all[state.levelIndex]
        state.enemies.toList().forEach { enemy ->
            enemy.reloadMs = (enemy.reloadMs - deltaMs).coerceAtLeast(0L)
            val plan = state.enemyPlans.getOrPut(enemy) { EnemyPlan() }
            plan.changeDirMs -= deltaMs
            plan.shootMs -= deltaMs
            if (plan.changeDirMs <= 0L) {
                enemy.direction = chooseEnemyDirection(enemy)
                plan.changeDirMs = random.nextLong(450L, 1200L)
            }
            val moved = tryMoveTank(enemy, enemy.direction, enemy.speed / 60f)
            if (!moved) {
                enemy.direction = chooseEnemyDirection(enemy, forceRandom = true)
                plan.changeDirMs = random.nextLong(220L, 760L)
            }
            if (plan.shootMs <= 0L && enemy.reloadMs <= 0L) {
                addBullet(enemy, false)
                enemy.reloadMs = level.enemyShootDelayMs
                plan.shootMs = random.nextLong((level.enemyShootDelayMs * 0.65f).toLong(), (level.enemyShootDelayMs * 1.55f).toLong())
            }
        }
    }

    private fun chooseEnemyDirection(enemy: Tank, forceRandom: Boolean = false): Direction {
        if (!forceRandom) {
            val base = state.baseCell
            if (random.nextFloat() < 0.48f) return Direction.Down
            if (random.nextFloat() < 0.22f) return if (base.first + .5f < enemy.x) Direction.Left else Direction.Right
        }
        return Direction.playable[random.nextInt(Direction.playable.size)]
    }

    private fun updateBullets() {
        val activeBullets = state.bullets.toList()
        activeBullets.forEach { bullet ->
            val bulletIndex = state.bullets.indexOfFirst { it === bullet }
            if (bulletIndex == -1) return@forEach
            bullet.x += bullet.direction.dx * bullet.speed / 60f
            bullet.y += bullet.direction.dy * bullet.speed / 60f
            val hit = handleBulletHit(bullet)
            val updatedBulletIndex = state.bullets.indexOfFirst { it === bullet }
            if (hit && updatedBulletIndex != -1) state.bullets.removeAt(updatedBulletIndex)
        }
    }

    private fun handleBulletHit(b: Bullet): Boolean {
        if (b.x < 0f || b.y < 0f || b.x >= width || b.y >= height) return true
        val tx = floor(b.x).toInt()
        val ty = floor(b.y).toInt()
        when (state.tiles[ty][tx]) {
            TileType.Brick -> {
                state.tiles[ty][tx] = TileType.Empty
                state.explosions += Explosion(tx + .5f, ty + .5f)
                beep(ToneGenerator.TONE_PROP_NACK, 55)
                return true
            }
            TileType.Metal -> return true
            TileType.Base -> {
                state.status = GameStatus.GameOver
                state.explosions += Explosion(tx + .5f, ty + .5f, 700L)
                beep(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
                return true
            }
            TileType.Empty -> Unit
        }
        if (b.fromPlayer) {
            val enemy = state.enemies.firstOrNull { rectsOverlap(b.x - .06f, b.y - .06f, .12f, .12f, it.x, it.y, .74f, .74f) }
            if (enemy != null) {
                state.enemies.remove(enemy)
                state.enemyPlans.remove(enemy)
                state.destroyedEnemies++
                state.explosions += Explosion(enemy.x + .37f, enemy.y + .37f)
                beep(ToneGenerator.TONE_PROP_ACK, 70)
                return true
            }
        } else if (rectsOverlap(b.x - .06f, b.y - .06f, .12f, .12f, state.player.x, state.player.y, .74f, .74f)) {
            state.player.lives--
            state.explosions += Explosion(state.player.x + .37f, state.player.y + .37f)
            beep(ToneGenerator.TONE_SUP_ERROR, 90)
            if (state.player.lives <= 0) state.status = GameStatus.GameOver else resetPlayer()
            return true
        }
        return false
    }

    private fun resetPlayer() {
        state.player.x = state.playerStart.first + .13f
        state.player.y = state.playerStart.second + .13f
        state.player.direction = Direction.Up
        state.bullets.removeAll { !it.fromPlayer }
    }

    private fun updateExplosions(deltaMs: Long) {
        state.explosions.forEach { it.ttlMs -= deltaMs }
        state.explosions.removeAll { it.ttlMs <= 0L }
    }

    private fun addBullet(tank: Tank, fromPlayer: Boolean) {
        val cx = tank.x + .37f + tank.direction.dx * .36f
        val cy = tank.y + .37f + tank.direction.dy * .36f
        state.bullets += Bullet(cx, cy, tank.direction, fromPlayer)
    }

    private fun tryMoveTank(tank: Tank, direction: Direction, step: Float): Boolean {
        val nx = tank.x + direction.dx * step
        val ny = tank.y + direction.dy * step
        if (!canOccupy(nx, ny, tank)) return false
        tank.x = nx
        tank.y = ny
        return true
    }

    private fun canOccupy(x: Float, y: Float, ignore: Tank?): Boolean {
        if (x < 0f || y < 0f || x + .74f > width || y + .74f > height) return false
        val minX = floor(x).toInt()
        val maxX = floor(x + .73f).toInt()
        val minY = floor(y).toInt()
        val maxY = floor(y + .73f).toInt()
        for (ty in minY..maxY) for (tx in minX..maxX) if (state.tiles[ty][tx] != TileType.Empty) return false
        val allTanks = state.enemies + state.player
        for (other in allTanks) {
            if (other !== ignore && rectsOverlap(x, y, .74f, .74f, other.x, other.y, .74f, .74f)) return false
        }
        return true
    }

    private val width: Int get() = Levels.all[state.levelIndex].width
    private val height: Int get() = Levels.all[state.levelIndex].height

    private fun beep(toneType: Int, durationMs: Int) {
        runCatching { tone?.startTone(toneType, durationMs) }
    }
}
