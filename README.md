# DevAtlas

Образовательное приложение для разработчиков: темы с деревом подтем, страницы со схемами и отметка
«Просмотрено». Тему добавляют ссылкой (папка Яндекс.Диска, GitHub, zip), дальше приложение работает
без сети.

Статус: в разработке, версия 0.1 ещё не выпущена. Сейчас готов каркас: дизайн-система, навигация
с пустыми экранами, выбор оформления «Светлая / Тёмная / Как в системе».

## Платформы

Kotlin Multiplatform + Compose Multiplatform. Собирается только Android (от Android 10).
Логика, данные, навигация, интерфейс и дизайн-система лежат в общем коде, но iOS не собирается
и не проверяется: целей iOS в проекте нет.

## Сборка

Нужны JDK 21 и Android SDK (путь — в `local.properties` или `ANDROID_HOME`).

```
./gradlew :app:assembleDebug
./gradlew detekt
./gradlew :detekt-rules:test :shared:testAndroidHostTest
```

APK отладочной сборки — `app/build/outputs/apk/debug/`. В отладочной сборке в настройках есть
каталог компонентов дизайн-системы в светлой и тёмной теме.

## Устройство

| Модуль | Что внутри |
|---|---|
| `:app` | точка входа Android: `Application`, `Activity`, системный сплеш, иконка |
| `:shared` | общий код приложения: граф DI, навигация, экраны, настройки, база |
| `:designsystem` | токены, иконки и компоненты; единственное место, где используется Material3 |
| `:detekt-rules` | свои правила detekt: цвета, отступы и Material3 только в дизайн-системе |
| `tools/themecheck` | место под проверку и упаковку папки темы (появится позже) |

Библиотеки: Compose Multiplatform, Navigation Compose, Metro (DI), Room, DataStore, okio, Ktor,
kotlinx-serialization, Coil 3. Версии — `gradle/libs.versions.toml`.

## Лицензия

MIT, см. [LICENSE](LICENSE).
