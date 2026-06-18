# Тапалка с уровнями

Что добавлено:

- экран игры на Jetpack Compose;
- 10 уровней с растущей целью по тапам;
- награды монетами за прохождение уровня;
- сила тапа увеличивается каждые 3 уровня;
- сохранение прогресса в SharedPreferences;
- кнопка сброса прогресса;
- русское название приложения: «Тапалка».

Главный файл логики:

`app/src/main/java/com/example/app1/MainActivity.kt`

Чтобы изменить баланс игры, отредактируй список `GameLevels` в `MainActivity.kt`:

```kotlin
private val GameLevels = listOf(
    TapLevel(number = 1, targetTaps = 20, rewardCoins = 10),
    TapLevel(number = 2, targetTaps = 35, rewardCoins = 15),
    // ...
)
```

Важно: исходный архив содержал только модуль `app`, без корневых файлов Gradle-проекта (`settings.gradle`, `gradlew`, `gradle/libs.versions.toml`). Поэтому этот архив тоже является модулем. Открывай его внутри своего Android Studio проекта или замени файлы в существующем проекте.
