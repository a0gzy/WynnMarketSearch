# Wynn Market Search

**Мод для Wynncraft с улучшенным поиском на рынке**

![WynnMarketSearch GUI](https://i.imgur.com/1muxM4V.png)

## Описание

Данная модификация предназначена специально для сервера **Wynncraft** и значительно упрощает процесс поиска товаров на внутриигровом рынке. Благодаря улучшенному интерфейсу и расширенным функциям поиска вы сможете быстрее находить нужные предметы и совершать выгодные покупки.

## Особенности

- 🎯 **Автоматическое открытие** — GUI поиска открывается автоматически при появлении сообщения от рынка
- 🔍 **Умный поиск** — мгновенный поиск предметов по названию с поддержкой частичных совпадений
- 🎨 **Цветовая индикация** — предметы отображаются цветом в соответствии с их редкостью
- 📦 **Иконки предметов** — все предметы имеют свои иконки с кастомными текстурами Wynncraft
- ⚡ **Асинхронная загрузка** — данные загружаются из API без задержек игры
- 📱 **Адаптивный интерфейс** — GUI одинаково выглядит при любом значении gui scale

## Цвета редкости

| Редкость | Цвет |
|----------|------|
| Normal | Серый |
| Unique | Жёлтый |
| Rare | Светло-фиолетовый |
| Legendary | Бирюзовый |
| Fabled | Красный |
| Mythic | Тёмно-фиолетовый |
| Set | Зелёный |

## Установка

1. Установите **Fabric Loader** для Minecraft 1.21.11
2. Скачайте мод и поместите `.jar` файл в папку `mods`
3. Установите зависимости:
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [Cloth Config](https://modrinth.com/mod/cloth-config)
   - [Mod Menu](https://modrinth.com/mod/modmenu) (опционально, для настроек)

## Настройки

Настройки доступны через **Mod Menu** → **WynnMarketSearch**:

| Параметр | Описание | По умолчанию |
|----------|----------|--------------|
| Market Search | Включить поиск при появлении сообщения | ✅ |
| Auto Focus | Автофокус поля поиска при открытии GUI | ✅ |

## Использование

1. Откройте рынок на сервере Wynncraft
2. Нажмите на предмет для продажи
3. Когда появится сообщение `Type the item name or type 'cancel' to cancel:`, мод автоматически откроет GUI поиска
4. Начните вводить название предмета
5. Выберите нужный предмет из списка кликом или нажмите Enter для отправки первого результата
6. Для отмены введите `cancel` или закройте GUI через ESC

## Требования

- **Minecraft**: 1.21.11
- **Fabric Loader**: ≥ 0.18.4
- **Java**: 21+

## Зависимости

- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config](https://modrinth.com/mod/cloth-config)
- [Mod Menu](https://modrinth.com/mod/modmenu) (опционально)

## Компиляция

```bash
# Клонирование репозитория
git clone https://github.com/a0g/WynnMarketSearch.git
cd WynnMarketSearch

# Сборка
./gradlew build

# Запуск клиента для тестирования
./gradlew runClient
```

Скомпилированный `.jar` файл будет находиться в `build/libs/`.

## Лицензия

[CC0 1.0 Universal](LICENSE) — Public Domain

## Ссылки

- [Исходный код](https://github.com/a0gzy/WynnMarketSearch)
- [Wynncraft API](https://api.wynncraft.com/v3/item/database?fullResult)
- [Отчёт об ошибке](https://github.com/a0gzy/WynnMarketSearch/issues)
