package com.example.app1.game

import kotlin.math.abs

enum class Direction(val dx: Int, val dy: Int) {
    Up(0, -1), Down(0, 1), Left(-1, 0), Right(1, 0);

    companion object {
        val playable = listOf(Up, Down, Left, Right)
    }
}

enum class TileType { Empty, Brick, Metal, Base }

enum class GameStatus { Menu, Playing, LevelWon, GameOver, Victory }

open class Tank(
    var x: Float,
    var y: Float,
    var direction: Direction,
    var speed: Float,
    var lives: Int = 1,
    var reloadMs: Long = 0L,
    val isPlayer: Boolean = false
)

data class Bullet(
    var x: Float,
    var y: Float,
    val direction: Direction,
    val fromPlayer: Boolean,
    val speed: Float = 8.4f
)

data class Explosion(var x: Float, var y: Float, var ttlMs: Long = 260L)

class Enemy(
    x: Float,
    y: Float,
    direction: Direction,
    speed: Float
) : Tank(x, y, direction, speed, lives = 1, isPlayer = false)

data class EnemyPlan(var changeDirMs: Long = 0L, var shootMs: Long = 0L)

data class Level(
    val map: List<String>,
    val enemyCount: Int,
    val enemySpeed: Float,
    val enemyShootDelayMs: Long,
    val enemySpawnDelayMs: Long
) {
    val width: Int get() = map.first().length
    val height: Int get() = map.size
}

data class GameState(
    val levelIndex: Int,
    var status: GameStatus = GameStatus.Playing,
    val tiles: MutableList<MutableList<TileType>>,
    var player: Tank,
    val enemies: MutableList<Tank> = mutableListOf(),
    val enemyPlans: MutableMap<Tank, EnemyPlan> = java.util.IdentityHashMap(),
    val bullets: MutableList<Bullet> = mutableListOf(),
    val explosions: MutableList<Explosion> = mutableListOf(),
    val enemySpawns: List<Pair<Int, Int>>,
    val playerStart: Pair<Int, Int>,
    val baseCell: Pair<Int, Int>,
    var enemiesLeftToSpawn: Int,
    var destroyedEnemies: Int = 0,
    var spawnTimerMs: Long = 0L,
    var inputDirection: Direction? = null
) {
    val remainingEnemies: Int get() = enemiesLeftToSpawn + enemies.size
}

fun rectsOverlap(ax: Float, ay: Float, aw: Float, ah: Float, bx: Float, by: Float, bw: Float, bh: Float): Boolean {
    return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by
}

fun distanceManhattan(aX: Float, aY: Float, bX: Float, bY: Float): Float = abs(aX - bX) + abs(aY - bY)
