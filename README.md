# DevAtlas

Образовательное приложение для разработчиков: темы с деревом подтем, страницы со схемами и отметка
«Просмотрено». Тему добавляют ссылкой (папка Яндекс.Диска, GitHub, zip), дальше приложение работает
без сети.

Статус: в разработке, версия 0.1 ещё не выпущена. Сейчас готовы каркас с навигацией, дизайн-система на
Material 3 Expressive с цветами из обоев, формат темы с проверкой и тема-пример.

## Платформы

Kotlin Multiplatform + Compose Multiplatform. Собирается только Android (от Android 10).
Логика, данные, навигация, интерфейс и дизайн-система лежат в общем коде, но iOS не собирается
и не проверяется: целей iOS в проекте нет.

## Сборка

Нужны JDK 21 и Android SDK (путь — в `local.properties` или `ANDROID_HOME`).

```
./gradlew :app:assembleDebug
./gradlew detekt
./gradlew :detekt-rules:test :shared:testAndroidHostTest :themeformat:jvmTest :tools:themecheck:test
./gradlew :app:connectedDebugAndroidTest
```

Последняя команда — e2e-тесты на подключённом телефоне или эмуляторе.

APK отладочной сборки — `app/build/outputs/apk/debug/`. В отладочной сборке в настройках есть
каталог компонентов дизайн-системы в светлой и тёмной теме.

## Устройство

| Модуль | Что внутри |
|---|---|
| `:app` | точка входа Android: `Application`, `Activity`, системный сплеш, иконка |
| `:shared` | общий код приложения: граф DI, навигация, экраны, настройки, база |
| `:designsystem` | тема Material 3 Expressive, иконки и компоненты; единственное место, где используется Material3 |
| `:themeformat` | формат темы: манифест, разбор страниц и схем, проверка, безопасная распаковка архива |
| `:tools:themecheck` | проверка и упаковка темы из командной строки |
| `:detekt-rules` | свои правила detekt: цвета, отступы и Material3 только в дизайн-системе |

Библиотеки: Compose Multiplatform, Navigation Compose, Metro (DI), Room, DataStore, okio, Ktor,
kotlinx-serialization, Coil 3. Версии — `gradle/libs.versions.toml`.

## Как сделать тему

Формат темы описан в [docs/theme-format.md](docs/theme-format.md), пример — папка
[sample-theme](sample-theme). Проверить свою тему и собрать архив:

```
./gradlew :tools:themecheck:run --args="путь/к/теме --zip тема.zip"
```

## Лицензия

MIT, см. [LICENSE](LICENSE).
