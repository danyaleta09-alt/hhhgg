## r173-camera-slide-up (versionCode 173)
- **Камера:** полностью убрал container-transform / expand-morph (белая вспышка, потеря скруглений, обрезания, лаги).
  Теперь открывается **простым плавным слайдом снизу** (translationY 1→0) + лёгкий scrim.
  Easing: CubicBezier(0.22, 1, 0.36, 1), in 380ms / out 300ms.
  Больше никаких bounds/Rect, onGloballyPositioned, CameraExpandOverlay.
  CameraCaptureScreen остался как был (OPEN_DELAY 380ms совпадает с анимацией).
- versionCode 171→173, versionName r173-camera-slide-up.

## r159-slider-glass-reset (versionCode 159)
- Слайдер воды: дизайн как у Telegram «звёздная реакция» — толстая скруглённая полоса, заливка-капсула, белый knob внутри трека. Ход плавный (fraction за пальцем), значение с шагом 50 мл.
- После «Добавить» слайдер сбрасывается на 250 мл — нельзя спамить одним тапом.
- Стакан: полностью скруглённый U-силуэт (мягкие верх и дно), без острых углов.
- versionCode 158→159.

## r158-glass-slider-statusbar (versionCode 158)
- **История воды / статус-бар:** пересобрал scaffold 1:1 как Appearance/Bindings/Notifications — `verticalScroll` → `windowInsetsPadding(statusBars)` → `padding(top=6)` внутри Column. Предыдущий порядок (insets до scroll, без top-pad) оставлял артефакт у статус-бара.
- **Стакан:** минималистичный flat-силуэт — ровные стенки, мягкое дно, сплошная заливка водой снизу. Без обводки, волны, градиента, блика.
- **Слайдер воды:** убран step-snap во время драга. Ползунок едет пиксель-в-пиксель за пальцем, мл обновляется каждый кадр (разрешение 1 мл).
- versionCode 157→158.

## r157-water-tg-avatar (versionCode 157)
- **Критический баг (ЦТ):** на «История воды» статус-бар был непрозрачным. ПРИЧИНА: outer Box имел только `fillMaxWidth()` → под статус-баром не рисовался page-bg. ФИКС: `fillMaxSize()` + opaque bg, как на всех остальных оверлеях.
- **Аватар Telegram:** getUserProfilePhotos часто возвращал пустой список (privacy) → photoUrl=null. Добавлен fallback через `getChat` (big/small_file_id). При старте AppState и при открытии Профиля, если user bound но photoUrl пустой — автоматический re-fetch. User-Agent на HTTP-запросах.
- **Скругления:** Card 28→20, CardSmall 20→16, Button 18→16, Chip 14→12 — ближе к Telegram-settings / референсам.
- **Редизайн Вода (Питание):**
  - SegmentedTabs компактнее (36dp, padding 3dp).
  - Кольцо заменено на наполняющийся стакан (WaterGlass) с волной.
  - Сетка быстрых кнопок → слайдер количества (50–1000 мл, шаг 50) с bubble-значением + кнопка «Добавить · N мл».
- versionCode 156→157, versionName r157-water-tg-avatar.

## r125-stories-ring-keep-on-dim (versionCode 125)
- dletniev (скрин): «сторис-ободок пропадает при затемнении кружков, он не должен исчезать».
- ПРИЧИНА: completion-dim (`dim` 1f->0.55f при выполнении привычки) применялся к ВСЕМУ graphicsLayer кружка в HabitCell — а внутри него рисуется и сосуд, и сторис-ободок (drawBehind, bg-color круг чуть больше сосуда), и непрозрачная bg-подложка. Поэтому при затемнении выцветал и разделитель/ободок → «пропадал».
- ФИКС: убрал `dim` из alpha внешнего слоя (теперь 1f / только collapse-fade для index>=visibleMiniCount). `dim` теперь применяется ТОЛЬКО к самому LiquidVessel (обёрнут в Box(Modifier.graphicsLayer{alpha=dim})). Затемняется только заливка+иконка кружка, а сторис-ободок и bg-подложка остаются полностью непрозрачными.
- versionCode 124->125.

## r124-no-progress-on-done (versionCode 124)
- dletniev: «у выполненных задач не должна отображаться полоса прогресса».
- PlanScreen.TaskCard: заливка-прогресс (полоса) теперь только `if (isLive && !isCompleted)`, где isCompleted = isCompletedOn(dateKey) || isPast. У выполненных/прошедших задач полосы нет. SubtaskTaskCard полосы не имеет.
- Также: попросил концепты «Сторис/Моменты» (фото еды/прогулок + пометки дня/веса). Сделал 4 разных направления (HTML, _design/stories-moments-concepts.html): 1 Сторис-вьюер, 2 Лента-дневник, 3 Коллекция-мозаика, 4 Капсула дня. Показал, жду выбор. Пока НЕ вшито в приложение.

## r119-restore (versionCode 123, code = clean r119)
- dletniev: «откат к r119 про остальное забудь». Восстановил исходники r119 целиком из доставленного wellness-src-v119.zip (re-download через files-pri URL). Вся фича миниатюры/колец (r120-r122) удалена. versionCode 119->123 чтобы ставилось поверх r122 (Android запрещает даунгрейд versionCode), versionName "r119-restore". Репо теперь = чистый r119.

# r119-docked-liquid — DONE (build verified, signed; needs on-device confirm)
- *Откат r118 + правка: свёрнутые кружки должны выглядеть как развёрнутые (жидкие), просто меньше —
  не сплошной цвет.* Убрал сплошной цветной overlay из r118. Вместо него подложил НЕПРОЗРАЧНЫЙ
  app-bg диск ПОД сосуд, проявляющийся только при сворачивании: сосуд сохраняет жидкий вид
  (заливка+иконка), но его прозрачная пустая часть становится непрозрачной (цвет фона) → на фоне
  топ-бара незаметно, а при нахлёсте перекрывает соседа, поэтому story-ободок чистый, а не
  просвечивающий. Border ободка оставил 5dp.
- versionCode 119, versionName r119-docked-liquid.

# r118-docked-opaque — DONE (build verified, signed; needs on-device confirm)
- *Баг (скрин): сторис-ободок свёрнутого кластера обрезанный/прозрачный, ужасный.* ПРИЧИНА: r113
  сделал свёрнутые мини-кружки тем же LiquidVessel, который показывает УРОВЕНЬ ЖИДКОСТИ
  (полупустой/прозрачный) — а по референсу rings-redesign мини-аватары должны быть НЕПРОЗРАЧНЫМИ.
  ФИКС: при сворачивании поверх жидкого сосуда плавно проявляется сплошной цветной диск + белая
  иконка (alpha = collapseProgress, всё в draw-фазе, без рекомпозиции) — только у докующихся кружков.
  Плюс story-ободок (bg-color border) был слишком тонкий: 3dp -> 5dp (после scale ~2.5dp, как в референсе).
- versionCode 118, versionName r118-docked-opaque.

# r117-collapse-smooth — DONE (build verified, signed; needs on-device confirm)
- *Запрос: само сворачивание должно быть плавнее.* Смягчил пружину снапа доковки: StiffnessMedium
  (1500) -> CollapseSnapStiffness 220 (между Low 50 и MediumLow 400), критически задемпфировано без
  отскока. Применено и в snapFling, и в settle-safety-net. Теперь кружки плавно «припарковываются»
  в миниатюру, без резкого щелчка. Плавный перелёт r116 оставлен.
- versionCode 117, versionName r117-collapse-smooth.

# r116-fly-smooth — DONE (build verified, signed; needs on-device confirm)
- *Запрос: кружки должны лететь плавнее при перелёте.* Заменил пружину (spring StiffnessMediumLow)
  на мягкий tween(560мс, FastOutSlowInEasing) в анимации visualIndex (HabitCell) — перелёт теперь
  плавный slow-in/slow-out, без «дёрганья». Длительность 560мс — если нужно быстрее/медленнее, легко
  крутить (durationMillis).
- versionCode 116, versionName r116-fly-smooth.
# r115-reorder-flash — DONE (build verified, signed; needs on-device confirm)
- *Баг (голосовое): при выполнении привычки кружок на миг ПОЯВЛЯЕТСЯ в конце, резко ИСЧЕЗАЕТ и
  только потом происходит перелёт.* Регрессия от r113 (моего глайда переупорядочивания).
  ПРИЧИНА: глайд делался через reorderX.snapTo(delta) в LaunchedEffect (асинхронно). После
  пересортировки кадр композиции ставил ячейку уже на НОВУЮ позицию (конец) с offset=0 → один кадр
  кружок виден в конце, затем snapTo откидывал его назад → «исчез» → потом анимация перелёта.
  ФИКС: вместо «offset от старого слота к 0» теперь анимируем visualIndex (Animatable) к реальному
  index, а offset = (visualIndex − index)·slot. На первом же кадре после пересортировки visualIndex
  ещё держит СТАРЫЙ слот, поэтому ячейка сразу рисуется на старом месте и плавно летит оттуда —
  кадра-вспышки на новой позиции больше нет.
- versionCode 115, versionName r115-reorder-flash. Подписан, R8. Эмулятора нет — нужен тест на устройстве.

# r114-collapse-snap — DONE (build verified, signed; needs on-device confirm)
- *Запрос: кружки сворачивались «по мере скролла» и могли застрять на половине прогресса, если
  остановить скролл. Надо чтобы сворачивались «от положения», а не по ходу скролла.*
  Геометрия полосы жёстко завязана на скролл (lift = +scroll − yTravel·t; вакантное место под
  полосой потребляется ровно за CollapseScrollDistance скролла), поэтому t обязан быть ∝ scroll —
  расцеплять нельзя (иначе дыра в layout). Решение: после отпускания палец-снап ВСЕГДА доводит до
  0 или collapsePx, поэтому промежуточного состояния не остаётся, а решение принимается по ПОЛОЖЕНИЮ.
  • Добавлен общий `collapseSnapTarget(value, velocity, collapsePx)`: прошёл >30% пути → доворот в
    полное сворачивание; меньше → откат в раскрытие; явный флик (>120 px/s) перебивает по своему
    направлению. Старый порог velocity был ±1 px/s — крошечный остаточный рывок мог развернуть жест.
  • snapFling и settle-safety-net теперь оба используют этот хелпер, пружина жёстче
    (StiffnessMediumLow → StiffnessMedium), чтобы доворот был быстрым и заметным.
  ОГРАНИЧЕНИЕ: пока палец на экране и тянет — это всё ещё скраб (иначе ломается layout). Полное
  «коммит даже во время перетаскивания без скраба» = крупный рефактор (липкий хедер вне скролла) —
  если нужно именно так, делаю отдельно.
- versionCode 114, versionName r114-collapse-snap. Подписан, R8. Эмулятора нет — нужен тест на устройстве.

# r113-strip-vessel — DONE (build verified, signed; needs on-device confirm)
- *Баг (СРОЧНО, остался после r112): кружки при сворачивании в миниатюру резко пропадают и снова появляются.*
  НАСТОЯЩАЯ ПРИЧИНА (r112-tilt была мимо): полоса привычек была на `LazyRow`. LazyRow ВЫГРУЖАЕТ
  (recycle/dispose) кружки, ушедшие за левый край при горизонтальном скролле полосы. Когда такой
  кружок выгружен, во время сворачивания его просто НЕТ в композиции → он отсутствует в мини-кластере
  («пропал»), и появляется обратно только когда полоса в конце сворачивания тихо сбрасывается к
  первому элементу (`scrollToItem(0)`) → кружок рекомпозится → «резко вернулся». Поэтому баг плавал:
  возникал тем заметнее, чем сильнее полоса была прокручена вбок.
  ФИКС: LazyRow → обычный `Row` + `horizontalScroll`. Теперь ВСЕ сосуды всегда в композиции, и морф в
  кластер всегда имеет их все — выгружать нечего. `hScrollOffset` теперь читает стабильный
  `ScrollState.value` (пиксели), а не firstVisibleItemIndex/offset.
  Глайд переупорядочивания (привычка выполнена → уезжает в конец) сохранён: в HabitCell добавлена
  относительная offset-анимация по смене slot-индекса (cellWidth+gap на слот), которая разрешается в 0
  и НЕ конфликтует с горизонтальным скроллом (в отличие от animateItem/animatePlacement).
- *Кружок на экране «Новая привычка» переделан под жидкий сосуд.* В RingHeroShelf (AddHabitScreen)
  ProgressRing+иконка заменены на тот же `LiquidVessel`, что и активные привычки на «План»: волнистая
  поверхность жидкости, наклон по сенсору, цвет-фон сосуда. Демонстрационная заливка ~0.62, чтобы волна
  читалась. Для шеринга `rememberDeviceTilt()` и `LiquidVessel` в PlanScreen.kt сделаны public (тот же
  пакет ui.screens), AddHabitScreen вызывает их напрямую.
- versionCode 113, versionName r113-strip-vessel. R8 minify+shrink, подписан. APK ~1.78MB.
- ВАЖНО: эмулятора/GPU в среде сборки нет — анимации визуально НЕ проверял, нужен тест на устройстве.

# r112-tilt-perf — DONE (build verified, signed; needs on-device confirm)
- *Баг #2 (СРОЧНО): кружки при сворачивании в миниатюру резко пропадают на ~секунду и снова появляются.*
- *Баг #1: при переходе с «План» на «Профиль»/«Питание» текст кружка на долю секунды выходит за границы / мигает.*
- ЕДИНАЯ ПРИЧИНА обоих: `rememberDeviceTilt()` возвращал сырой Float, который читался ПРЯМО в теле PlanScreen.
  Каждый сэмпл акселерометра (SENSOR_DELAY_GAME ≈ 50 Гц) рекомпозил ВЕСЬ тяжёлый экран «План» (список задач и т.д.).
  Когда этот шторм рекомпозиций совпадал с пружиной сворачивания (баг #2) или со слайдом смены вкладки (баг #1),
  главный поток захлёбывался → дропались кадры → кружки/текст «дёргались»/пропадали.
- ФИКС: `rememberDeviceTilt()` теперь отдаёт СТАБИЛЬНУЮ лямбду `() -> Float`; её вызывают ТОЛЬКО внутри
  маленьких LiquidVessel. Теперь сенсор рекомпозит лишь крошечные Canvas-сосуды, а не весь экран. Тот же
  паттерн с лямбдами, что уже используется в этом экране для scroll/collapse.
- Заодно: удалён мёртвый код HabitRow/HabitCircle/MiniHabitVessel (не вызывались, держали старую сигнатуру).
- versionCode 112, versionName r112-tilt-perf. R8 minify+shrink, подписан. APK ~1.79MB.
- ВАЖНО (среда сборки): нет /dev/kvm и GPU — эмулятор недоступен, анимации визуально не проверял.
- ВАЖНО (инструмент): file_edit в этой среде ДУБЛИРОВАЛ контент файла → правил Python-скриптом со строгой проверкой числа совпадений и wc -l.

# r95-subtasks-ui — DONE (2026-06-02)
- Subtask card (PlanScreen): collapsed = slightly pre-open, first item peeks and dissolves into a
  soft bottom fade; tap expands DOWNWARD (animateContentSize); fade animates out when open. Item
  label = max 2 lines then ellipsis.
- Subtask creation (AddTaskScreen.TaskSubtasksSubScreen): Notes-style single solid sheet, numbered
  squares per row, multi-line WRAPPING text (no overflow past edge), Enter = new numbered row,
  Backspace-on-empty deletes, long-press ⠿ grip to drag-reorder, X to delete. Blank rows trimmed on exit.
- Transitions (WellnessApp): nested push now matches top-level shared-axis (underlay slides in lockstep
  via nestedParallax + shiftFraction + Cover dim). Close-flash fixed: nestedParallax inits at 0f on POP
  so the screen popped back to is centered on frame 1 (no витрина flash on closing «Запланировать»).
- versionCode 95.

# r93-transitions — DONE (2026-06-02)
- *Мигание при «Запланировать» (и любом вложенном открытии).* Драйвер вложенных переходов (`nestedParallax`) теперь пересоздаётся при каждом изменении стека со значением 1f → входящий экран стартует за правым краем, а не показывается «на месте» один кадр перед слайдом. Это и было мигание.
- *Переход не тот, что в настройках («Сдвиг/Наплыв»).* Раньше родитель под вложенным экраном рисовался статично (пин x=0) ради фикса чёрного экрана — из-за этого вложенные переходы всегда выглядели как «Наплыв». Теперь родитель сдвигается в локстеп с входящим экраном (общий `nestedParallax` + `overlayHostShiftFraction`), как делает OverlayHost для верхнего уровня → вложенные переходы подчиняются выбранному стилю. Чёрного экрана нет (оба экрана движутся синхронно, фон залит). В режиме «Наплыв» добавлено затемнение уходящего родителя.
- *Витрина активити прыгает вверх при тапе по пункту.* Тап по строке пушил детальный экран → витрина перемонтировалась в слот «подложки» с новым scroll-state и сбрасывалась наверх. Добавил `SaveableStateHolder` (`overlayStateHolder`) с ключом по идентичности оверлея — теперь scroll витрины (и состояние других экранов) сохраняется при переходе между слотами «верх»/«подложка». Тот же класс бага, что когда-то чинили на экране прогресса целей.

# r92-carousel — DONE (2026-06-02)
- *«+» в шапке «план» без лага.* Меню создания (Привычка/Задача/Активити) больше не Popup-окно (его аллокация стоила кадр на открытии — это и были «лаги»). Теперь это in-composition оверлей (LAYER 4, full-screen Box + прозрачный скрим для тапа-вне), анимируется с первого кадра.
- *«+» — настоящий плюс.* Новая иконка `add-bold.svg` (две скруглённые рейки) вместо `add-circle-bold-duotone`. Кнопка больше не «круглая».
- *Экран «Новая активити» — карусель.* Заменил горизонтальный скролл на центрированную зацикленную snap-карусель (LazyRow + rememberSnapFlingBehavior, виртуально-бесконечный список, старт по центру выбранной). Центральная карточка увеличена и залита акцентом активити, боковые уменьшаются и тускнеют (graphicsLayer scale+alpha по дистанции до центра). Свайп/тап подгоняет карточку к центру; центральная = выбранная → подтягивает иконку+цвет+имя.
- *Привязка активити к задаче убрана.* Из создания ЗАДАЧИ удалена секция «АКТИВИТИ» и под-экран привязки; удалён `ActivityPicker.kt`, убран `TaskRoute.Activity`. Активити теперь создаются только на своём экране.
- *Новый пункт «Настройка» в создании активити.* В блоке «ПАРАМЕТРЫ» добавлен пункт `settings-bold-duotone` → выезжает `ActivityEditor` выбранной активити (фазы/цели/интервалы) тем же slide-оверлеем.

# r91-newflow — DONE (2026-06-02)
- *Новый флоу создания (по утверждённому концепту).* Убраны посекционные «+» у «Привычки» и «Расписание». Одна круглая «+» слева в шапке «план» → поповер-меню *Привычка / Задача / Активити* (`PlanCreateMenu`).
- *Отдельный экран «Новая активити»* (`AddActivityScreen` в AddTaskScreen.kt). Сверху — горизонтальная карусель карточек активити вместо предпросмотра задачи; выбор карточки подтягивает иконку+цвет+имя. У активити НЕТ выбора иконки и цвета (они заданы активити). Поля: имя, Время, Напоминания. Под капотом создаётся TaskItem с activityId → встаёт в план как ▶-карточка.
- *Баг #1 (мигание при «Запланировать»)* убран: путь «деталь → префилл задачи» заменён — «Запланировать» теперь открывает тот же `AddActivityScreen` с предвыбранной активити (нет отдельного prefilled-task экрана, нечему мигать).
- *Баг #2 (чёрный экран на карточке активити в витрине)* исправлен: подложка (родительский оверлей) теперь рендерится статически на x=0 без параллакс-сдвига. В стиле Push старый сдвиг уводил родителя за экран (-width), и кадры, где входящий экран ещё не закрыл 100%, показывали чёрный фон. Теперь родитель всегда заполняет фон.

# r89-clean — DONE (2026-06-02)
- *Кнопки-галочки без подложки.* `HeaderCheckButton` (галочка-подтверждение в заголовках всех экранов — ActivityEditor, AddTask, AddHabit и т.д.) больше не рисует акцентную плашку: только глиф `check-bold`, accent когда активна / muted когда disabled. Тач-таргет 44dp сохранён.
- *Кнопка сброса «Навбар» без подложки* — убрал круглый container-фон, осталась чистая иконка `restart-bold-duotone` (44dp таргет).
- *Аудит рывков/лагов (статический).* Прошёлся по коду: derivedStateOf везде в remember; на кэшируемых вкладках (CachedTabPager) нет infiniteRepeatable-анимаций (не жрут кадры в фоне); нет nested-lazy багов. Кодовая база уже хорошо оптимизирована (история r82/r85/r86). Точечных багов под статикой не нашёл — для конкретных рывков нужен профайл на устройстве / указание экрана.

# r88-fixes — DONE (2026-06-01)
- *Статус-бар обрезал контент на «План»* (n: статус-бар). r87 добавил `windowInsetsPadding(statusBars)` (клип) на LazyColumn → верхняя карточка резалась по линии статус-бара. Убрал клип; статус-бар inset свёрнут в высоту item-0 (header-reserve). Теперь контент уходит под прозрачный статус-бар edge-to-edge как на всех остальных экранах, без маски/скрима.
- *n5336: «+» новой задачи не работал при свёрнутых кружках.* Свёрнутый оверлей привычек (Row horizontalScroll + хит-боксы колец, остающиеся в позициях развёрнутого лэйаута) перехватывал тапы по кнопке «+» расписания под ним. Добавил `ringsInteractive = collapseProgress < 0.5f`; при свёрнутом состоянии снимается `horizontalScroll` со Strip, `noFeedbackClick` с колец, и «+» у «Привычки» не рендерится → полоса становится click-through.
- *n5803: на экране «Активити» нельзя было открыть настройки выбранной карточки* (⚙ заменялась галочкой). Убрал ⚙ со ВСЕХ карточек; настройки открываются долгим нажатием (`noFeedbackCombinedClick` + хаптик). Редактор теперь выезжает как `RoundedSlideOverlay` поверх `OverlayHost` — та же slide-анимация и свайп-назад, что и везде (раньше открывался инлайн без анимации).
- *n1365: кнопка сброса в «Навбар».* Добавил `AppState.resetNavbar()` (порядок Home/Nutrition/Plan/Profile + стартовый Home) и круглую кнопку-сброс (restart-bold-duotone) в правом верхнем углу заголовка экрана «Навбар».

# r63 — DONE ✅ (shipped, build verified, PDF delivered)
- Selectable transition style (Push="Сдвиг" / Cover="Наплыв") with live animated previews
- "Свайп назад" toggle
- Привязки moved Profile root → Другое
- Push smoothed 320→360ms
- magic-stick Solar icon, persistence, signed APK, src zip, official PDF report

## r86-cache — DONE (2026-06-01)
- "очень сильно лагает открытие экрана задач" + janky tab transitions. Root cause: AnimatedContent disposed+rebuilt each tab's whole composition on every navbar switch (SaveableStateProvider only restores rememberSaveable, not the composition) → heavy non-lazy PlanScreen recomposed every open, and the incoming screen was composed mid-slide.
- Fix: replaced AnimatedContent with CachedTabPager (WellnessApp.kt) — keeps every visited tab COMPOSED, parked off-screen with alpha-0 graphicsLayer (draws nothing while idle), only the from/to tabs animate a direct push (Animatable 0→1, TabPushMs/Easing). Re-entry instant, slide at full framerate. onSettledChange drives tabSettled (removed the delay() guess).
- First open of each screen per session still composes once (unavoidable), then cached.

## r85-perf — DONE (2026-06-01)
- Card colour JUMP on drag-to-close: r82's LocalOverlaySettled froze the wash to the transition's START frame (-8/0/0). After the screen was open a while the live перелив had advanced far → grabbing the card hard-cut the colour. Fix: freeze to CURRENT values via a remembered float holder written each live frame (ImmersiveBackground h/d/s, AuraBackground d/b). No jump.
- Scroll/general lag: frosted navbar (haze) re-blurred the backdrop every frame as the list slid under it. Fix: new AppState.contentScrolling flag (set by PlanScreen snapshotFlow on scrollState/hScrollState.isScrollInProgress, cleared onDispose) → Navbar drops the blur (blurAlpha→0, no haze sampling) while scrolling, fades it back 180ms at rest. Reuses the existing tab-slide blur-off path.
- Drag finger-lag: removing the heavy one-frame wash redraw at drag start (same fix as #1) smooths the start of the drag.
- Still inherent: tab-switch open cost = PlanScreen full recomposition (AnimatedContent disposes outgoing). Could cache the screen if he reports it's still bad.

## r82-smooth — DONE (2026-06-01)
- Fix card-open + swipe-close lag.
- Gate immersive wash/aura draw behind LocalOverlaySettled (freeze to static frame during sheet open/drag → no draw-phase contention; async open).
- BottomSheetOverlay drag now uses one conflated Channel + single ordered consumer (was scope.launch{snapTo} per pointer event → out-of-order jitter).

## r84-overlap — DONE (2026-06-01)
- FIX: overlapping Активити tasks vanished. ScheduleList showed only the FIRST live task (`firstOrNull{Live}`) — any other task whose time window covered "now" was in neither live/upcoming/past buckets and was silently dropped. Looked like "task with activity won't create" / "can't have several activity tasks at the same time".
- Fix: `live = sorted.filter{Live}` + `live.forEach{TaskCard}` in PlanScreen.kt → all concurrent tasks render.
