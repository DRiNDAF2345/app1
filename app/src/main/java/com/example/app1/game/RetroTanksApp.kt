package com.example.app1.game

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val PREFS = "retro_tanks_progress"
private const val KEY_LEVEL = "level"

@Composable
fun RetroTanksApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val engine = remember { GameEngine() }
    var screen by remember { mutableStateOf(GameStatus.Menu) }
    var openedLevel by remember { mutableIntStateOf(prefs.getInt(KEY_LEVEL, 0).coerceIn(0, Levels.all.lastIndex)) }
    var tick by remember { mutableIntStateOf(0) }

    fun launchLevel(index: Int) {
        engine.start(index)
        screen = GameStatus.Playing
        tick++
    }

    LaunchedEffect(screen) {
        var last = System.nanoTime()
        while (screen == GameStatus.Playing) {
            delay(16L)
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000L).coerceIn(1L, 40L)
            last = now
            engine.update(dt)
            tick = engine.version()
            when (engine.state.status) {
                GameStatus.LevelWon -> {
                    val nextOpen = (engine.state.levelIndex + 1).coerceAtMost(Levels.all.lastIndex)
                    if (nextOpen > openedLevel) {
                        openedLevel = nextOpen
                        prefs.edit().putInt(KEY_LEVEL, openedLevel).apply()
                    }
                    screen = if (engine.state.levelIndex == Levels.all.lastIndex) GameStatus.Victory else GameStatus.LevelWon
                }
                GameStatus.GameOver -> screen = GameStatus.GameOver
                else -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101114))
            .padding(14.dp)
    ) {
        when (screen) {
            GameStatus.Menu -> MainMenu(
                openedLevel = openedLevel,
                onPlay = { launchLevel(0) },
                onContinue = { launchLevel(openedLevel) },
                onReset = {
                    openedLevel = 0
                    prefs.edit().putInt(KEY_LEVEL, 0).apply()
                }
            )
            GameStatus.Playing -> GameScreen(engine = engine, tick = tick)
            GameStatus.LevelWon -> LevelWonScreen(
                level = engine.state.levelIndex + 1,
                onNext = { launchLevel((engine.state.levelIndex + 1).coerceAtMost(Levels.all.lastIndex)) }
            )
            GameStatus.Victory -> VictoryScreen(onMenu = { screen = GameStatus.Menu }, onRestart = { launchLevel(0) })
            GameStatus.GameOver -> GameOverScreen(onRestart = { launchLevel(engine.state.levelIndex) }, onMenu = { screen = GameStatus.Menu })
        }
    }
}

@Composable
private fun MainMenu(openedLevel: Int, onPlay: () -> Unit, onContinue: () -> Unit, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("РЕТРО ТАНЧИКИ", color = Color(0xFFFFD54F), fontSize = 33.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text("Оригинальная 2D аркада сверху", color = Color(0xFFB0BEC5), fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
        MenuButton("Играть", onPlay)
        MenuButton("Продолжить: уровень ${openedLevel + 1}", onContinue)
        MenuButton("Сбросить прогресс", onReset)
        Text("Защити базу, разбей кирпичи, переживи 5 уровней.", color = Color(0xFF78909C), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 22.dp))
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238), contentColor = Color(0xFFFFF8E1)),
        modifier = Modifier
            .fillMaxWidth(.78f)
            .padding(vertical = 6.dp)
            .height(52.dp)
    ) { Text(text, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GameScreen(engine: GameEngine, tick: Int) {
    val state = engine.state
    val level = Levels.all[state.levelIndex]
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HudText("❤ ${state.player.lives}")
            HudText("Уровень ${state.levelIndex + 1}")
            HudText("Враги ${state.remainingEnemies}")
        }
        Spacer(Modifier.height(8.dp))
        GameCanvas(state = state, level = level, modifier = Modifier.fillMaxWidth().aspectRatio(level.width.toFloat() / level.height.toFloat()).border(2.dp, Color(0xFF455A64)))
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            DPad(onDirection = engine::setDirection)
            FireButton(onFire = engine::firePlayer)
        }
    }
}

@Composable
private fun HudText(text: String) {
    Text(text, color = Color(0xFFFFF59D), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@Composable
private fun DPad(onDirection: (Direction?) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ControlButton("▲", Direction.Up, onDirection)
        Row {
            ControlButton("◀", Direction.Left, onDirection)
            Spacer(Modifier.size(56.dp))
            ControlButton("▶", Direction.Right, onDirection)
        }
        ControlButton("▼", Direction.Down, onDirection)
    }
}

@Composable
private fun ControlButton(label: String, direction: Direction, onDirection: (Direction?) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .padding(3.dp)
            .background(Color(0xFF263238), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF78909C), RoundedCornerShape(10.dp))
            .pointerInput(direction) {
                detectTapGestures(
                    onPress = {
                        onDirection(direction)
                        tryAwaitRelease()
                        onDirection(null)
                    }
                )
            }
    ) { Text(label, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun FireButton(onFire: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(104.dp)
            .background(Color(0xFF7B1F1F), RoundedCornerShape(52.dp))
            .border(3.dp, Color(0xFFFF8A65), RoundedCornerShape(52.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onFire() }) }
    ) { Text("ОГОНЬ", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black) }
}

@Composable
private fun LevelWonScreen(level: Int, onNext: () -> Unit) {
    CenterCard(title = "Уровень $level пройден", subtitle = "База защищена.") { MenuButton("Следующий уровень", onNext) }
}

@Composable
private fun GameOverScreen(onRestart: () -> Unit, onMenu: () -> Unit) {
    CenterCard(title = "Игра окончена", subtitle = "База пала или танк уничтожен.") {
        MenuButton("Заново", onRestart)
        MenuButton("В меню", onMenu)
    }
}

@Composable
private fun VictoryScreen(onMenu: () -> Unit, onRestart: () -> Unit) {
    CenterCard(title = "Победа!", subtitle = "Все 5 уровней очищены.") {
        MenuButton("Заново", onRestart)
        MenuButton("В меню", onMenu)
    }
}

@Composable
private fun CenterCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2328)), modifier = Modifier.fillMaxWidth(.92f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(22.dp)) {
                Text(title, color = Color(0xFFFFD54F), fontSize = 27.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(subtitle, color = Color(0xFFCFD8DC), modifier = Modifier.padding(vertical = 14.dp), textAlign = TextAlign.Center)
                content()
            }
        }
    }
}

@Composable
private fun GameCanvas(state: GameState, level: Level, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color(0xFF171A1D))) {
        val cell = size.width / level.width
        val boardH = cell * level.height
        drawRect(Color(0xFF171A1D), size = Size(size.width, boardH))
        for (y in 0 until level.height) for (x in 0 until level.width) {
            val left = x * cell
            val top = y * cell
            drawRect(Color(0xFF23282C), topLeft = Offset(left, top), size = Size(cell, cell), style = Stroke(width = 1f))
            when (state.tiles[y][x]) {
                TileType.Brick -> drawBrick(left, top, cell)
                TileType.Metal -> drawMetal(left, top, cell)
                TileType.Base -> drawBase(left, top, cell)
                TileType.Empty -> Unit
            }
        }
        state.enemies.forEach { drawTank(it, cell, Color(0xFFE57373), Color(0xFFB71C1C)) }
        drawTank(state.player, cell, Color(0xFF81C784), Color(0xFF1B5E20))
        state.bullets.forEach { drawBullet(it, cell) }
        state.explosions.forEach { drawExplosion(it, cell) }
    }
}

private fun DrawScope.drawBrick(left: Float, top: Float, cell: Float) {
    drawRect(Color(0xFF9E4B2E), Offset(left + cell * .06f, top + cell * .08f), Size(cell * .88f, cell * .84f))
    drawLine(Color(0xFF5D2C1C), Offset(left + cell * .08f, top + cell * .5f), Offset(left + cell * .92f, top + cell * .5f), strokeWidth = 2f)
    drawLine(Color(0xFF5D2C1C), Offset(left + cell * .5f, top + cell * .08f), Offset(left + cell * .5f, top + cell * .5f), strokeWidth = 2f)
    drawLine(Color(0xFF5D2C1C), Offset(left + cell * .28f, top + cell * .5f), Offset(left + cell * .28f, top + cell * .92f), strokeWidth = 2f)
    drawLine(Color(0xFF5D2C1C), Offset(left + cell * .72f, top + cell * .5f), Offset(left + cell * .72f, top + cell * .92f), strokeWidth = 2f)
}

private fun DrawScope.drawMetal(left: Float, top: Float, cell: Float) {
    drawRect(Color(0xFF607D8B), Offset(left + cell * .05f, top + cell * .05f), Size(cell * .9f, cell * .9f))
    drawRect(Color(0xFF263238), Offset(left + cell * .18f, top + cell * .18f), Size(cell * .64f, cell * .64f), style = Stroke(width = 3f))
}

private fun DrawScope.drawBase(left: Float, top: Float, cell: Float) {
    drawRect(Color(0xFFFFD54F), Offset(left + cell * .18f, top + cell * .22f), Size(cell * .64f, cell * .58f))
    val roof = Path().apply {
        moveTo(left + cell * .1f, top + cell * .35f)
        lineTo(left + cell * .5f, top + cell * .08f)
        lineTo(left + cell * .9f, top + cell * .35f)
        close()
    }
    drawPath(roof, Color(0xFFFF8F00))
    drawRect(Color(0xFF5D4037), Offset(left + cell * .42f, top + cell * .52f), Size(cell * .16f, cell * .28f))
}

private fun DrawScope.drawTank(tank: Tank, cell: Float, body: Color, dark: Color) {
    val l = tank.x * cell
    val t = tank.y * cell
    val s = cell * .74f
    drawRect(dark, Offset(l, t + s * .1f), Size(s * .22f, s * .8f))
    drawRect(dark, Offset(l + s * .78f, t + s * .1f), Size(s * .22f, s * .8f))
    drawRect(body, Offset(l + s * .18f, t + s * .18f), Size(s * .64f, s * .64f))
    drawCircle(dark, radius = s * .18f, center = Offset(l + s * .5f, t + s * .5f))
    val barrelStart = Offset(l + s * .5f, t + s * .5f)
    val barrelEnd = when (tank.direction) {
        Direction.Up -> Offset(l + s * .5f, t - s * .12f)
        Direction.Down -> Offset(l + s * .5f, t + s * 1.12f)
        Direction.Left -> Offset(l - s * .12f, t + s * .5f)
        Direction.Right -> Offset(l + s * 1.12f, t + s * .5f)
    }
    drawLine(Color(0xFFFFFDE7), barrelStart, barrelEnd, strokeWidth = s * .16f)
}

private fun DrawScope.drawBullet(bullet: Bullet, cell: Float) {
    drawCircle(if (bullet.fromPlayer) Color(0xFFFFF176) else Color(0xFFFF7043), radius = cell * .105f, center = Offset(bullet.x * cell, bullet.y * cell))
}

private fun DrawScope.drawExplosion(explosion: Explosion, cell: Float) {
    val progress = explosion.ttlMs / 260f
    val r = cell * (.18f + (1f - progress.coerceIn(0f, 1f)) * .42f)
    drawCircle(Color(0xFFFFF176), radius = r, center = Offset(explosion.x * cell, explosion.y * cell), style = Stroke(width = 4f))
    drawCircle(Color(0xFFFF7043), radius = r * .55f, center = Offset(explosion.x * cell, explosion.y * cell))
}
