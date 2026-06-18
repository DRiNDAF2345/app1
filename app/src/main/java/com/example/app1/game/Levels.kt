package com.example.app1.game

object Levels {
    val all = listOf(
        Level(
            map = listOf(
                "E....M....E..",
                "..BB...BB....",
                "..M....M..B..",
                ".............",
                ".BBB..BBB..B.",
                ".....M.......",
                "..B.....B....",
                "....BBB......",
                ".............",
                "..M..P..M....",
                ".....BBB.....",
                "....BHB......"
            ), enemyCount = 5, enemySpeed = 1.28f, enemyShootDelayMs = 1700L, enemySpawnDelayMs = 1100L
        ),
        Level(
            map = listOf(
                "E..B.M.B..E..",
                "BB.B...B.BB..",
                "...M.B.M.....",
                ".B.....B..B..",
                ".B.BBB.B..B..",
                ".....M.......",
                "..BB...BB....",
                "....B.B......",
                "..M.....M....",
                ".....P.......",
                "...BBBBB.....",
                "....BHB......"
            ), enemyCount = 7, enemySpeed = 1.44f, enemyShootDelayMs = 1450L, enemySpawnDelayMs = 950L
        ),
        Level(
            map = listOf(
                "E.B..M..B.E..",
                "..B.B.B.B....",
                "BB..M.M..BB..",
                ".....B.......",
                ".BBB...BBB.B.",
                "..M..B..M....",
                "....BBB......",
                ".BB.....BB...",
                "....M.M......",
                "..B..P..B....",
                "..BBBBBBB....",
                "....BHB......"
            ), enemyCount = 9, enemySpeed = 1.58f, enemyShootDelayMs = 1250L, enemySpawnDelayMs = 850L
        ),
        Level(
            map = listOf(
                "E.B.M.B.M.E..",
                "B.B...B...B..",
                "..M.BBB.M....",
                "BB.......BB..",
                "..BBB.BBB....",
                "M....B....M..",
                "..BB...BB....",
                ".B..M.M..B...",
                "....BBB......",
                "..M..P..M....",
                "..BBBBBBB....",
                "....BHB......"
            ), enemyCount = 11, enemySpeed = 1.72f, enemyShootDelayMs = 1050L, enemySpawnDelayMs = 760L
        ),
        Level(
            map = listOf(
                "E.B.MBM.B.E..",
                "B.B.B.B.B.B..",
                "..M..B..M....",
                "BB.B...B.BB..",
                "..BBB.BBB....",
                "M.B.....B.M..",
                "..BB.M.BB....",
                ".B...B...B...",
                "..M.BBB.M....",
                "..B..P..B....",
                "..BBBBBBB....",
                "....BHB......"
            ), enemyCount = 14, enemySpeed = 1.9f, enemyShootDelayMs = 850L, enemySpawnDelayMs = 650L
        )
    )
}
