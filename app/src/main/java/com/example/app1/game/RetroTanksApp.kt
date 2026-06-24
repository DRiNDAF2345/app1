package com.example.app1.game

import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

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
            GameStatus.Playing -> GameScreen(
                engine = engine,
                tick = tick,
                onDirection = { direction ->
                    engine.setDirection(direction)
                    tick = engine.version()
                },
                onFire = {
                    engine.firePlayer()
                    tick = engine.version()
                }
            )
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 360.dp || maxHeight < 620.dp
        val titleSize = if (compact) 27.sp else 33.sp
        val buttonWidth = if (compact) .92f else .78f
        val buttonHeight = if (compact) 46.dp else 52.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "РЕТРО ТАНЧИКИ",
                color = Color(0xFFFFD54F),
                fontSize = titleSize,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                "2D аркада сверху",
                color = Color(0xFFB0BEC5),
                fontSize = if (compact) 13.sp else 15.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = if (compact) 18.dp else 28.dp),
                textAlign = TextAlign.Center
            )
            MenuButton("Играть", onPlay, buttonWidth, buttonHeight)
            MenuButton("Продолжить: ур. ${openedLevel + 1}", onContinue, buttonWidth, buttonHeight)
            MenuButton("Сбросить прогресс", onReset, buttonWidth, buttonHeight)
            Text(
                "Защити базу, разбей кирпичи, переживи 5 уровней.",
                color = Color(0xFF78909C),
                textAlign = TextAlign.Center,
                fontSize = if (compact) 13.sp else 14.sp,
                modifier = Modifier.padding(top = if (compact) 14.dp else 22.dp)
            )
        }
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit, widthFraction: Float = .78f, height: Dp = 52.dp) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238), contentColor = Color(0xFFFFF8E1)),
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .padding(vertical = 6.dp)
            .height(height)
    ) {
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (height < 50.dp) 14.sp else 16.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun GameScreen(engine: GameEngine, tick: Int, onDirection: (Direction?) -> Unit, onFire: () -> Unit) {
    val state = engine.state
    val level = Levels.all[state.levelIndex]
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 360.dp || maxHeight < 640.dp
        val controlSize = if (compact) 48.dp else 56.dp
        val fireSize = if (compact) 88.dp else 104.dp
        val dPadHeight = controlSize * 3f
        val hudHeight = if (compact) 22.dp else 28.dp
        val boardMaxHeight = (maxHeight - hudHeight - dPadHeight - 28.dp).coerceAtLeast(150.dp)
        val boardMaxWidth = boardMaxHeight * (level.width.toFloat() / level.height.toFloat())
        val boardWidth = minOf(maxWidth, boardMaxWidth)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hudHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudText("Жизни ${state.player.lives}", compact)
                HudText("Ур. ${state.levelIndex + 1}", compact)
                HudText("Враги ${state.remainingEnemies}", compact)
            }
            GameCanvas(
                state = state,
                level = level,
                frame = tick,
                modifier = Modifier
                    .width(boardWidth)
                    .aspectRatio(level.width.toFloat() / level.height.toFloat())
                    .border(2.dp, Color(0xFF455A64))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPad(onDirection = onDirection, buttonSize = controlSize)
                FireButton(onFire = onFire, size = fireSize)
            }
        }
    }
}

@Composable
private fun HudText(text: String, compact: Boolean) {
    Text(
        text,
        color = Color(0xFFFFF59D),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = if (compact) 13.sp else 16.sp,
        maxLines = 1
    )
}

@Composable
private fun DPad(onDirection: (Direction?) -> Unit, buttonSize: Dp) {
    var activeDirection by remember { mutableStateOf<Direction?>(null) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val padSize = buttonSize * 3f
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(padSize)
            .background(Color(0xFF1E2A30), RoundedCornerShape(28.dp))
            .border(2.dp, Color(0xFF607D8B), RoundedCornerShape(28.dp))
            .onSizeChanged { layoutSize = it }
            .joystickPress(layoutSize) { direction ->
                if (direction != activeDirection) {
                    activeDirection = direction
                    onDirection(direction)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val ring = size.minDimension * .32f
            drawCircle(Color(0xFF263238), radius = size.minDimension * .44f, center = center)
            drawCircle(Color(0xFF101820), radius = ring, center = center, style = Stroke(width = 5f))
            drawLine(Color(0xFF78909C), Offset(center.x, size.height * .14f), Offset(center.x, size.height * .86f), strokeWidth = 4f)
            drawLine(Color(0xFF78909C), Offset(size.width * .14f, center.y), Offset(size.width * .86f, center.y), strokeWidth = 4f)
            val knobCenter = when (activeDirection) {
                Direction.Up -> Offset(center.x, center.y - ring)
                Direction.Down -> Offset(center.x, center.y + ring)
                Direction.Left -> Offset(center.x - ring, center.y)
                Direction.Right -> Offset(center.x + ring, center.y)
                null -> center
            }
            drawCircle(Color(0xFFFFD54F), radius = size.minDimension * .13f, center = knobCenter)
            drawCircle(Color(0xFFFF8F00), radius = size.minDimension * .07f, center = knobCenter)
        }
        Text("▲", color = Color.White, fontSize = if (buttonSize < 52.dp) 18.sp else 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        Text("▼", color = Color.White, fontSize = if (buttonSize < 52.dp) 18.sp else 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
        Text("◀", color = Color.White, fontSize = if (buttonSize < 52.dp) 18.sp else 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
        Text("▶", color = Color.White, fontSize = if (buttonSize < 52.dp) 18.sp else 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
    }
}

@Composable
private fun FireButton(onFire: () -> Unit, size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(Color(0xFF7B1F1F), RoundedCornerShape(52.dp))
            .border(3.dp, Color(0xFFFF8A65), RoundedCornerShape(52.dp))
            .firePress(onFire)
    ) { Text("ОГОНЬ", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = if (size < 96.dp) 13.sp else 15.sp) }
}

private fun Modifier.joystickPress(size: IntSize, onDirection: (Direction?) -> Unit): Modifier =
    pointerInteropFilter { event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                onDirection(event.toJoystickDirection(size))
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onDirection(null)
                true
            }
            else -> true
        }
    }

private fun MotionEvent.toJoystickDirection(size: IntSize): Direction? {
    if (size.width <= 0 || size.height <= 0) return null
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val dx = x - centerX
    val dy = y - centerY
    val deadZone = minOf(size.width, size.height) * .13f
    if (abs(dx) < deadZone && abs(dy) < deadZone) return null
    return if (abs(dx) > abs(dy)) {
        if (dx < 0f) Direction.Left else Direction.Right
    } else {
        if (dy < 0f) Direction.Up else Direction.Down
    }
}

private fun Modifier.firePress(onFire: () -> Unit): Modifier =
    pointerInteropFilter { event ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            onFire()
        }
        true
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 360.dp || maxHeight < 620.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2328)), modifier = Modifier.fillMaxWidth(if (compact) .98f else .92f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(if (compact) 16.dp else 22.dp)) {
                    Text(title, color = Color(0xFFFFD54F), fontSize = if (compact) 23.sp else 27.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(subtitle, color = Color(0xFFCFD8DC), modifier = Modifier.padding(vertical = 14.dp), textAlign = TextAlign.Center, fontSize = if (compact) 14.sp else 16.sp)
                    content()
                }
            }
        }
    }
}

@Composable
private fun GameCanvas(state: GameState, level: Level, frame: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color(0xFF171A1D))) {
        if (frame < 0) return@Canvas
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
