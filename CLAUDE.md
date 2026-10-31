# DevAtlas — правила проекта для Claude

Решения прожарки и план версии 0.1 — `dev-tools/devatlas.md` в личной базе знаний разработчика; общие правила
pet-проектов — `dev-tools/pet-projects.md` там же. Здесь — устройство кода и правила работы с ним.

## Платформа

- KMP, цели — только Android. С AGP 9 KMP-библиотеки собираются плагином
  `com.android.kotlin.multiplatform.library`: Android-цель объявляется блоком `kotlin { android { } }`,
  а не `androidTarget()`. `:app` — обычный модуль `com.android.application` (KMP-плагин в одном модуле с
  приложением AGP 9 не поддерживает), в нём только точка входа.
- Исключение — `:themeformat`: у него есть ещё цель `jvm()`, её берёт JVM-утилита `:tools:themecheck`.
- Всё, что не зависит от платформы, — в `commonMain`. Платформенное — интерфейсом в `commonMain`,
  реализацией в `androidMain` (пример — `PlatformBindings` / `AndroidPlatformBindings`). Платформенное внутри
  Compose-функции дизайн-системы (цвета из обоев) — `expect`/`actual`: ему нужен `LocalContext`, а не граф.
- В `commonMain` — только мультиплатформенные библиотеки (у артефакта есть iOS-цели). Платформенные
  библиотеки (движок Ktor `okhttp`, `activity-compose`, `core-splashscreen`) — только в `androidMain` и `:app`.

## Модули

| Модуль | Зачем |
|---|---|
| `:app` | `Application` (держит граф), `MainActivity`, системный сплеш, ресурсы Android, e2e-тесты |
| `:shared` | граф Metro, навигация, экраны, ViewModel, настройки, Room, проверки запуска |
| `:designsystem` | тема M3 Expressive, токены, иконки, компоненты, каталог компонентов |
| `:themeformat` | формат темы: манифест, разборщик HTML и SVG, валидатор, безопасная распаковка zip |
| `:tools:themecheck` | JVM CLI: проверка папки или архива темы, упаковка в zip |
| `:detekt-rules` | свои правила detekt (JVM) |

Почему один `:shared`, а не модуль на фичу: в 0.1 экранов немного, связи между ними плотные (тема →
содержание → страница → прогресс), а каждый KMP-модуль добавляет к сборке заметную цену конфигурации.
Фичи разделены пакетами по слоям (`settings/domain`, `settings/data`, `ui/settings`). Модуль на фичу
выделяется, когда пакет фичи станет самодостаточным и его сборка начнёт мешать.

Почему формат — отдельный модуль: тот же валидатор нужен JVM-утилите `themecheck`, а `:shared` собирается
только под Android и тянет Compose и Room. `:themeformat` — чистый Kotlin (kotlinx-serialization, okio) без
Compose и DI, цели `android` и `jvm`.

## DI — Metro

- Граф `AppGraph` (`AppScope`) в `commonMain`, создаётся в `androidMain` функцией
  `createAndroidAppGraph(context, appInfo, endpoints)`; `:app` Metro не подключает.
- Адреса сети — `NetworkEndpoints` в графе: в приложении `NetworkEndpoints.Production`, в e2e — адрес
  MockWebServer. Новый сетевой адрес добавляется сюда, а не строкой в коде.
- Реализации: интерфейс + `Impl`, `@ContributesBinding(AppScope::class)` и `@Inject` на классе.
  Синглтоны — `@SingleIn(AppScope::class)`. Провайдеры сторонних объектов — `@BindingContainer`:
  DataStore в `StorageBindings`, база в `DatabaseBindings`.
- Проверки запуска — `@ContributesIntoSet(AppScope::class)` реализации `StartupCheck`; сплеш гоняет весь набор.
- ViewModel — `@Inject`-конструктор, граф отдаёт функцию `() -> …ViewModel`, экран берёт её через
  `viewModel { graph.xxxViewModel() }`.

## Хранение

- Настройки — DataStore Preferences, файл `settings.preferences_pb` в `filesDir`: режим оформления и
  «Цвета из обоев» (`Appearance`).
- База — Room KMP с `BundledSQLiteDriver`, `devatlas.db`, схемы — `shared/schemas` (в git).
  До выпуска 0.1 миграций не пишем, но каждая правка схемы поднимает `version` в `DevAtlasDatabase`: debug-сборка
  тогда пересоздаёт базу (`fallbackToDestructiveMigration` в `DatabaseBindings`), а правка схемы без смены версии
  даёт на сплеше ошибку проверки базы. После выпуска — только миграциями.

## Дизайн-система

- Material 3 Expressive: `MaterialExpressiveTheme`, `MotionScheme.expressive()`, компоненты Expressive
  (кнопки с морфингом форм, `ToggleButton` в группе, `SegmentedListItem`, `LoadingIndicator`, волнистый
  прогресс). Expressive открыт в `org.jetbrains.compose.material3:material3:1.12.0-alpha03` (androidx 1.5.0-alpha22) —
  пара к CMP 1.12.2 по release notes JetBrains; в 1.9.0 он `internal`. Версию material3 поднимать вместе с CMP
  по таблице в release notes; MaterialKolor собран под ту же версию material3.
- Компоненты `Atlas*` — тонкие обёртки над M3; `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` — только
  внутри `:designsystem`. Типографика — `AtlasTheme.typography` (шкала M3 и `emphasized`), формы —
  `AtlasTheme.shapes` (шкала углов и формы `MaterialShapes`), цвета — `AtlasTheme.colors` = роли `ColorScheme`.
- Цвет: по умолчанию цвета из обоев (Android 12+, `AtlasTheme.wallpaperColorsAvailable`), настройка «Цвета из
  обоев»; без них — схема из seed-цвета через MaterialKolor (`AtlasSeedScheme`, спецификация 2025).
- Меньше рамок: уровни разделяются тоном, а не обводкой. Фон экранов и шапки — `surfaceContainer`, строки и
  карточки — `surface`; `OutlinedButton`, обводки и разделители в компонентах не используются.
- Экраны берут только `AtlasTheme.*` и компоненты `Atlas*`. Своё правило detekt (`config/detekt/detekt.yml`,
  набор `devatlas`) запрещает вне `:designsystem` вызовы `Color(...)`, литералы `N.dp`, Material3 и MaterialKolor
  (импорт или полное имя). `material3` подключён к `:designsystem` как `api` — тип `ColorScheme` публичный.
- Иконки — Material Symbols (outlined, 24) в `AtlasIcons`, путь из SVG как есть, viewBox `0 -960 960 960`.
  Новая иконка — путь из `google/material-design-icons/symbols/web/<имя>/materialsymbolsoutlined/<имя>_24px.svg`.
  Цвет — по умолчанию `LocalContentColor`, иначе роль схемы.
- Новый компонент — сразу в каталог (`ComponentCatalog`): обе темы, с цветами из обоев и без.

## Формат темы

- Источник истины — `docs/theme-format.md`; правится вместе с кодом `:themeformat` и страницей сайта.
  Якоря разделов (`HelpSection`) — часть контракта: на них ведут сообщения проверки.
- Разметка — свой токенизатор (`markup/`): HTML и SVG в одном коде, позиции строк и столбцов для сообщений,
  без зависимостей. Страница разбирается в модель `PageDocument` (блоки и строчные элементы) — её рисует экран.
- Проверка папки — `ThemeValidator`, распаковка — `ThemeArchiveExtractor` (свой разбор каталога zip поверх
  okio: сырые имена записей нужны, чтобы отклонять `../`, а не молча нормализовать). Распаковка — во временную
  папку: при ошибке она удаляется целиком.
- Тема-пример — `sample-theme/`, проходит `themecheck --strict` без замечаний; тест `SampleThemeTest`
  проверяет, что она использует все блоки, стили и виды ссылок. Сломанная фикстура —
  `themeformat/src/jvmTest/resources/fixtures/broken-theme`.

## E2E-тесты

- `:app/src/androidTest`: Compose UI-тесты на настоящей `MainActivity` с настоящим графом Metro. Тестовый
  `Application` (`DevAtlasTestRunner`) подставляет адреса `MockWebServer` и выключает удержание системного
  сплеша (иначе Compose-тест не дождётся простоя). Test Orchestrator с `clearPackageData`: каждый тест — свой
  процесс и чистые данные. `ExternalIntentsRule` подменяет внешние интенты (браузер, PDF, «Поделиться»).
- Сценарии ходят по экранам через семантику: тексты, `contentDescription`, `testTag` корня и свойства
  оформления (`ui/semantics/AppSemantics.kt`). Состояние после действия — ожиданием (`waitFor…`), а не мгновенной
  проверкой: запись настроек асинхронная.
- Тред, добавивший или поменявший экран или поток, в том же треде пишет или правит его e2e.

## Сборка и проверки

- Сборки на ПК разработчика — через очередь ресурсов: `python "<dev-config>/claude/hooks/slots.py" run -- ./gradlew …`
  (папка вне слотов другого проекта: ритуальной чистки нет, ждёт только чужую сборку). `adb` — через `slots.py device -- adb …`.
- Перед коммитом: `./gradlew detekt :detekt-rules:test :designsystem:testAndroidHostTest
  :shared:testAndroidHostTest :themeformat:jvmTest :tools:themecheck:test :app:assembleSslDebug :app:assembleNoSslDebug
  :app:connectedSslDebugAndroidTest` (e2e — на телефоне, обёртка сама
  берёт устройство).
- CI — GitHub Actions на push в `main`: detekt, unit-тесты, `themecheck --strict` по `sample-theme` (архив —
  артефакт), APK; отдельный job — e2e на эмуляторе API 34.
- Варианты сборки — `debug` (суффикс `.debug`, каталог компонентов) и `release`.

## Правила кода

- Комментариев и KDoc нет. Один класс — один файл (кроме вложенных в `sealed`).
- Ничего корпоративного в коде, README и примерах.
