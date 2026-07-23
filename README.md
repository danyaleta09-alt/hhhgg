# Wellness — Android (Kotlin · Jetpack Compose)

Полностью нативное Android-приложение, повторяющее прототип 1:1 (тёмная/светлая темы, 5 экранов, плавающий навбар-островок, плавные анимации, иконки Solar из Iconify).

## Стек

- Kotlin 1.9.22
- Jetpack Compose (BOM 2024.02.02)
- Material 3
- Coil + Coil-SVG (рендер Solar SVG-иконок из `assets/icons/`)
- Manrope (variable + Medium/SemiBold/Bold/ExtraBold)
- Android Gradle Plugin 8.3.2 / Gradle 8.7
- minSdk 26, target 34

## Структура

```
app/src/main/
├── AndroidManifest.xml
├── java/com/wellness/app/
│   ├── MainActivity.kt
│   ├── ui/
│   │   ├── WellnessApp.kt          // корневая Compose-навигация
│   │   ├── theme/                  // цвета, типографика, формы
│   │   ├── icons/SolarIcon.kt      // загрузка SVG из assets
│   │   ├── components/             // карточки, навбар, кольца, сегменты, шиты
│   │   ├── screens/                // Home / Nutrition / Plan / Trackers / Profile
│   │   ├── sheets/                 // добавление воды-еды-привычек-задач-веса-сна
│   │   └── state/AppState.kt       // глобальный state (Compose)
│   └── ...
├── assets/icons/*.svg              // 50+ Solar duotone-иконок
├── res/font/manrope_*.ttf
└── res/values, res/values-night/
```

## Локальная сборка

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew :app:assembleDebug
# или
./gradlew :app:assembleRelease
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

В `.github/workflows/build.yml` настроен пайплайн, который:
1. Поднимает JDK 17 + Android SDK
2. Кэширует Gradle
3. Собирает Debug и Release APK
4. Публикует артефакты `wellness-debug-apk` и `wellness-release-apk`

Запускается на push в `main`/`master`, на pull-request и вручную через `workflow_dispatch`.

## Дизайн-токены

| Токен | Тёмная | Светлая |
|-------|--------|---------|
| Фон | `#000000` | `#F1F1F3` |
| Контейнер | `#181818` | `#FFFFFF` |
| Текст | `#F4F4F5` | `#18181B` |
| Muted | `#8A8A92` | `#8A8A92` |

Акцентные цвета на выбор: мятный `#7CD992`, голубой `#5AA7FF`, фиолетовый `#B084F5`, оранжевый `#FF8C5A`, розовый `#FF6B9D`, жёлтый `#FFD166`.

## Принципы

- Никаких ripple-эффектов, изменения цвета или масштаба на нажатие — все интерактивные элементы используют `NoFeedbackButton` / `Modifier.noFeedbackClick`.
- Никаких обводок и разделителей — карточки разнесены отступами и фоном.
- Анимации переходов между экранами, появлением шитов и подсветкой навбара — `spring`-based, плавные.
- Все иконки приходят из набора Solar (Iconify), хранятся локально как SVG и рендерятся через Coil-SVG c `ColorFilter.tint`.

## Привязки (Telegram)

Профиль → «Привязки» открывает экран привязки Telegram-аккаунта через бот [@letifybot](https://t.me/letifybot).

Поток авторизации, без бэкенда:

1. Экран генерирует короткий сессионный токен.
2. Открывает Telegram-приложение (или web-клиент) на deep-link `t.me/letifybot?start=<token>`.
3. Пользователь жмёт «Запустить» — бот получает `/start <token>`.
4. Приложение long-poll'ит `getUpdates` Bot API напрямую и подхватывает соответствующее сообщение.
5. Идентификатор / имя / username сохраняются в `SharedPreferences` через `TelegramBindingStore` и переживают перезапуск.

Реализация:
- `app/src/main/java/com/wellness/app/telegram/TelegramAuth.kt` — long-poll, deep-link, sendMessage подтверждение.
- `app/src/main/java/com/wellness/app/telegram/TelegramBindingStore.kt` — персист.
- `app/src/main/java/com/wellness/app/ui/screens/BindingsScreen.kt` — UI: idle / polling / bound состояния, copy-link, отмена, отвязка.

Манифест получает `<uses-permission android:name="android.permission.INTERNET"/>` и `<queries>` для `tg://` / `https://t.me`.

> Токен бота встроен в клиент. Это **не** production-практика — её следует заменить на полноценный бэкенд, который держит токен и подписывает запросы за пользователя.
