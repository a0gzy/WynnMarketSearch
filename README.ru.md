# WynnMarketSearch

Fabric-мод для Wynncraft: вместо поиска в чате - открывается GUI с поиском, фильтрами и быстрыми слотами.

[English](README.md)

![GUI](img/gui.png)

## Что делает

Когда сервер пишет `Type the item name or type 'cancel' to cancel:`, мод
открывает поисковую панель. Предметы тянутся с собственного бэкенда, который
обновляет базу Wynncraft раз в сутки — никаких API-ключей у клиента.

## Управление

| Действие | Что |
|---|---|
| **ЛКМ** по предмету | отправить имя в чат |
| **ПКМ** по предмету | закрепить в нижний слот |
| **СКМ** по предмету | избранное (★ справа, всплывает наверх) |
| **ЛКМ** по слоту / истории | повторно отправить в чат |
| **ПКМ** по слоту / истории | убрать |
| **Enter** | отправить введённый текст как есть |
| **ESC** | отмена |

Правая панель сортирует результаты A→Z / Z→A; нижняя полоса вмещает до 18
закреплённых предметов, история хранит последние 20 запросов.

## Установка

1. Fabric Loader под **Minecraft 1.21.11**
2. Положи `.jar` мода и зависимости в `mods/`:
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [Cloth Config](https://modrinth.com/mod/cloth-config)
   - [Mod Menu](https://modrinth.com/mod/modmenu) (опционально, открывает настройки)

## Настройки (Mod Menu → WynnMarketSearch)

| Опция | Что |
|---|---|
| Enable Market Search | открывать GUI по триггерному сообщению |
| Auto-focus Search Box | фокус ввода при открытии |
| Item Database API URL | переопределить на свой бэкенд |
| Show History Panel | показывать историю |
| Show Pinned Slots | показывать нижние слоты |
| Show Instructions Box | показывать подсказку в углу |

## Команды

- `/wms` — открыть GUI вручную
- `/wms add <name>` — добавить личный кастомный предмет
- `/wms del <name>` — удалить
- `/wms list` — показать личные кастомы
- `/wms reload` — форсированный fetch с бэкенда (минуя локальный кэш)

## Сборка

```bash
./gradlew build       # → build/libs/wms-*.jar
./gradlew runClient   # тестовый клиент
```

## Лицензия

[CC0 1.0](LICENSE) — Public Domain.

## Ссылки

- [Исходники](https://github.com/a0gzy/WynnMarketSearch)
- [Issues](https://github.com/a0gzy/WynnMarketSearch/issues)
