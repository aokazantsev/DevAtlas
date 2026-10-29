# DevAtlas — правила проекта для Claude

Решения прожарки и план версии 0.1 — `C:\Users\aokaz\ai-memory\dev-tools\devatlas.md`; общие правила
pet-проектов — `dev-tools/pet-projects.md` там же. Здесь — устройство кода и правила работы с ним.

## Платформа

- KMP, цели — только Android. С AGP 9 KMP-библиотеки собираются плагином
  `com.android.kotlin.multiplatform.library`: Android-цель объявляется блоком `kotlin { android { } }`,
  а не `androidTarget()`. `:app` — обычный модуль `com.android.application` (KMP-плагин в одном модуле с
  приложением AGP 9 не поддерживает), в нём только точка входа.
- Всё, что не зависит от платформы, — в `commonMain`. Платформенное — интерфейсом в `commonMain`,
  реализацией в `androidMain` (пример — `PlatformBindings` / `AndroidPlatformBindings`).
- В `commonMain` — только мультиплатформенные библиотеки (у артефакта есть iOS-цели). Платформенные
  библиотеки (движок Ktor `okhttp`, `activity-compose`, `core-splashscreen`) — только в `androidMain` и `:app`.

## Модули

| Модуль | Зачем |
|---|---|
| `:app` | `Application` (держит граф), `MainActivity`, системный сплеш, ресурсы Android |
| `:shared` | граф Metro, навигация, экраны, ViewModel, настройки, Room, проверки запуска |
| `:designsystem` | токены, иконки, компоненты, каталог компонентов |
| `:detekt-rules` | свои правила detekt (JVM) |
| `tools/themecheck` | место под `:tools:themecheck` (тред 3) |

Почему один `:shared`, а не модуль на фичу: в 0.1 экранов немного, связи между ними плотные (тема →
содержание → страница → прогресс), а каждый KMP-модуль добавляет к сборке заметную цену конфигурации.
Фичи разделены пакетами по слоям (`settings/domain`, `settings/data`, `ui/settings`). Модуль на фичу
выделяется, когда пакет фичи станет самодостаточным и его сборка начнёт мешать.

## DI — Metro

- Граф `AppGraph` (`AppScope`) в `commonMain`, создаётся в `androidMain` функцией
  `createAndroidAppGraph(context, appInfo)`; `:app` Metro не подключает.
- Реализации: интерфейс + `Impl`, `@ContributesBinding(AppScope::class)` и `@Inject` на классе.
  Синглтоны — `@SingleIn(AppScope::class)`. Провайдеры сторонних объектов (DataStore, база) —
  `@BindingContainer` `StorageBindings`.
- Проверки запуска — `@ContributesIntoSet(AppScope::class)` реализации `StartupCheck`; сплеш гоняет весь набор.
- ViewModel — `@Inject`-конструктор, граф отдаёт функцию `() -> …ViewModel`, экран берёт её через
  `viewModel { graph.xxxViewModel() }`.

## Хранение

- Настройки — DataStore Preferences, файл `settings.preferences_pb` в `filesDir`.
- База — Room KMP с `BundledSQLiteDriver`, `devatlas.db`, схемы — `shared/schemas` (в git).
  До выпуска 0.1 миграций не пишем, но каждая правка схемы поднимает `version` в `DevAtlasDatabase`: debug-сборка
  тогда пересоздаёт базу (`fallbackToDestructiveMigration` в `StorageBindings`), а правка схемы без смены версии
  даёт на сплеше ошибку проверки базы. После выпуска — только миграциями.

## Дизайн-система

- Экраны берут только `AtlasTheme.*` токены и компоненты `Atlas*`. Своё правило detekt
  (`config/detekt/detekt.yml`, набор `devatlas`) запрещает вне `:designsystem` вызовы `Color(...)`,
  литералы `N.dp` и Material3 (импорт или полное имя).
- Палитра — оттенки серого + один акцент (`accent`); `success` совпадает с акцентом, `error` — с
  основным текстом, ошибку показывает иконка.
- Иконки — Material Symbols (outlined, 24) в `AtlasIcons`, путь из SVG как есть, viewBox `0 -960 960 960`.
  Новая иконка — путь из `google/material-design-icons/symbols/web/<имя>/materialsymbolsoutlined/<имя>_24px.svg`.
  Цвет — всегда `tint` из токена.
- Новый компонент — сразу в каталог (`ComponentCatalog`), он показывает обе темы.

## Сборка и проверки

- Сборки на ПК разработчика — через очередь ресурсов: `python "C:/Users/aokaz/dev-config/claude/hooks/slots.py" run -- ./gradlew …`
  (папка вне слотов banki: ритуальной чистки нет, ждёт только чужую сборку). `adb` — через `slots.py device -- adb …`.
- Перед коммитом: `./gradlew detekt :detekt-rules:test :shared:testAndroidHostTest :app:assembleDebug`.
- CI — GitHub Actions на push в `main`: detekt, тесты, debug-APK.
- Варианты сборки — `debug` (суффикс `.debug`, каталог компонентов) и `release`.

## Правила кода

- Комментариев и KDoc нет. Один класс — один файл (кроме вложенных в `sealed`).
- Ничего корпоративного в коде, README и примерах.
