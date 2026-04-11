package me.a0g.gui;

import me.a0g.Wms;
import me.a0g.api.WynnApi;
import me.a0g.api.WynnItem;
import me.a0g.config.ModConfig;
import me.a0g.gui.widgets.ItemButtonWidget;
import me.a0g.gui.widgets.SearchInputWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Экран поиска предметов (аналог WEScreen из WynnExtras)
 */
public class SearchGuiScreen extends Screen {
    // Базовые размеры (при targetScaleFactor = 2.0)
    private static final int BASE_WIDTH = 400;
    private static final int BASE_HEIGHT = 280;
    private static final int BASE_ITEM_HEIGHT = 36;
    private static final int BASE_SEARCH_HEIGHT = 20;
    private static final int MAX_VISIBLE_ITEMS = 7;

    // Целевой scale factor для фиксированного визуального размера
    private static final double TARGET_SCALE_FACTOR = 2.0;

    private final WynnApi api;
    private boolean shouldCancel = true;

    // Масштабирование
    private double scaleFactor = 1.0;
    private int scaledWidth, scaledHeight, scaledItemHeight, scaledSearchHeight;
    private int scaledX, scaledY;

    // Виджеты
    private SearchInputWidget searchBox;
    private final List<ItemButtonWidget> itemButtons = new ArrayList<>();

    // Данные поиска
    private List<WynnItem> currentResults = new ArrayList<>();
    private CompletableFuture<Void> searchFuture;

    public SearchGuiScreen(WynnApi api) {
        this(api, true);
    }

    public SearchGuiScreen(WynnApi api, boolean shouldCancel) {
        super(Text.literal("WynnMarketSearch"));
        this.api = api;
        this.shouldCancel = shouldCancel;
    }

    @Override
    protected void init() {
        super.init();

        // Вычисляем scale factor для фиксированного размера GUI
        // Целимся в gui scale = 2, но ограничиваем чтобы GUI влезало в экран
        double windowScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        double targetScaleFactor = 2.0 / windowScale;

        // Ограничиваем scaleFactor чтобы GUI влезало в экран (с запасом 10%)
        double maxScaleX = (width * 0.9) / BASE_WIDTH;
        double maxScaleY = (height * 0.9) / BASE_HEIGHT;
        this.scaleFactor = Math.min(targetScaleFactor, Math.min(maxScaleX, maxScaleY));

        // Вычисляем масштабированные размеры для экрана
        this.scaledWidth = (int) (BASE_WIDTH * scaleFactor);
        this.scaledHeight = (int) (BASE_HEIGHT * scaleFactor);
        this.scaledItemHeight = (int) (BASE_ITEM_HEIGHT * scaleFactor);
        this.scaledSearchHeight = (int) (BASE_SEARCH_HEIGHT * scaleFactor);

        // Позиция GUI по центру (на экране)
        this.scaledX = (width - scaledWidth) / 2;
        this.scaledY = (height - scaledHeight) / 2;

        // Базовые координаты (в масштабированной системе координат)
        int baseX = (int) (scaledX / scaleFactor);
        int baseY = (int) (scaledY / scaleFactor);

        // Очищаем старые кнопки
        itemButtons.clear();

        // Создаем поле поиска (в базовых координатах!)
        searchBox = new SearchInputWidget(
                baseX + 4,
                baseY + 4,
                BASE_WIDTH - 8,
                BASE_SEARCH_HEIGHT,
                this::onSearchTextChanged,
                scaleFactor
        );

        // Автофокус - устанавливаем сразу после создания
        if (ModConfig.get().autoFocus) {
            searchBox.setFocused(true);
        }

        // Создаем кнопки для предметов (в базовых координатах!)
        int buttonYOffset = 28 + 4;
        for (int i = 0; i < MAX_VISIBLE_ITEMS; i++) {
            ItemButtonWidget button = new ItemButtonWidget(
                    baseX + 4,
                    baseY + buttonYOffset + (i * BASE_ITEM_HEIGHT),
                    BASE_WIDTH - 8,
                    BASE_ITEM_HEIGHT - 4,
                    this::onItemSelected
            );
            itemButtons.add(button);
        }
    }

    private void onSearchTextChanged(String text) {
        // Отменяем предыдущий поиск
        if (searchFuture != null && !searchFuture.isDone()) {
            searchFuture.cancel(true);
        }

        // Очищаем кнопки
        clearItemButtons();

        if (text.isEmpty()) {
            currentResults = new ArrayList<>();
            return;
        }

        // Выполняем поиск асинхронно
        searchFuture = CompletableFuture.runAsync(() -> {
            currentResults = api.searchItems(text);

            // Обновляем GUI в главном потоке
            MinecraftClient.getInstance().execute(this::updateItemButtons);
        });
    }

    private void clearItemButtons() {
        for (ItemButtonWidget button : itemButtons) {
            button.setItem(null);
        }
    }

    private void updateItemButtons() {
        for (int i = 0; i < itemButtons.size() && i < currentResults.size(); i++) {
            itemButtons.get(i).setItem(currentResults.get(i));
        }

        // Очищаем остальные кнопки
        for (int i = currentResults.size(); i < itemButtons.size(); i++) {
            itemButtons.get(i).setItem(null);
        }
    }

    private void onItemSelected(WynnItem item) {
        if (item == null) return;

        shouldCancel = false;
        String itemName = item.getNameToChat();

        if (!itemName.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.networkHandler.sendChatMessage(itemName);
            }
        }

        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Скорректированные координаты мыши для масштабированного GUI
        int scaledMouseX = (int) (mouseX / scaleFactor);
        int scaledMouseY = (int) (mouseY / scaleFactor);

        // Базовые координаты для рендеринга (в масштабированной системе)
        int baseX = (int) (scaledX / scaleFactor);
        int baseY = (int) (scaledY / scaleFactor);

        // Темный фон на весь экран (без масштабирования)
        context.fill(0, 0, width, height, 0x88000000);

        // Сохраняем матрицу и применяем масштабирование
        context.getMatrices().pushMatrix();
        context.getMatrices().scale((float) scaleFactor, (float) scaleFactor);

        // Фон GUI
        context.fill(baseX, baseY, baseX + BASE_WIDTH, baseY + BASE_HEIGHT, 0xFF1A1A1A);

        // Верхняя панель (где поле поиска)
        context.fill(baseX, baseY, baseX + BASE_WIDTH, baseY + 28, 0xFF2D2D2D);

        // Рамка вокруг всего GUI
        int borderColor = 0xFF555555;
        context.drawHorizontalLine(baseX, baseX + BASE_WIDTH, baseY, borderColor);
        context.drawHorizontalLine(baseX, baseX + BASE_WIDTH, baseY + BASE_HEIGHT, borderColor);
        context.drawVerticalLine(baseX, baseY, baseY + BASE_HEIGHT, borderColor);
        context.drawVerticalLine(baseX + BASE_WIDTH, baseY, baseY + BASE_HEIGHT, borderColor);

        // Разделитель между полем поиска и списком
        context.drawHorizontalLine(baseX, baseX + BASE_WIDTH, baseY + 28, 0xFF444444);

        // Рендер кнопок предметов
        for (ItemButtonWidget button : itemButtons) {
            button.draw(context, scaledMouseX, scaledMouseY, delta);
        }

        // Рендер поля поиска поверх кнопок
        searchBox.draw(context, scaledMouseX, scaledMouseY, delta);

        // Статус загрузки
        drawStatusText(context, baseX, baseY);

        // Восстанавливаем матрицу
        context.getMatrices().popMatrix();

        // Рендерим tooltip и другие элементы поверх
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStatusText(DrawContext context, int x, int y) {
        if (api.isLoading()) {
            Text loadingText = Text.literal("Loading items...");
            context.drawTextWithShadow(
                    textRenderer,
                    loadingText,
                    x + BASE_WIDTH / 2 - textRenderer.getWidth(loadingText) / 2,
                    y + BASE_HEIGHT - 20,
                    0xFFFFAA00
            );
        } else if (!api.isLoaded()) {
            Text errorText = Text.literal("Items not loaded yet");
            context.drawTextWithShadow(
                    textRenderer,
                    errorText,
                    x + BASE_WIDTH / 2 - textRenderer.getWidth(errorText) / 2,
                    y + BASE_HEIGHT - 20,
                    0xFFFF5555
            );
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        // Скорректированные координаты мыши для масштабированного GUI
        double scaledMouseX = click.x() / scaleFactor;
        double scaledMouseY = click.y() / scaleFactor;
        int button = click.button();

        // Сначала обработка поля поиска
        if (searchBox.mouseClicked(scaledMouseX, scaledMouseY, doubleClick)) {
            return true;
        }

        // Затем обработка кнопок предметов
        for (ItemButtonWidget btn : itemButtons) {
            if (btn.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        
        // Обработка Enter
        if ((keyCode == 257 || keyCode == 335) && !searchBox.isEmpty()) {
            String text = searchBox.getText();
            if (!text.isEmpty()) {
                shouldCancel = false;
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.networkHandler.sendChatMessage(text);
                }
                close();
                return true;
            }
        }

        if (searchBox.keyPressed(input)) {
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchBox.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        if (shouldCancel) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.currentScreen == this) {
                client.player.networkHandler.sendChatMessage("cancel");
            }
        }
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void removed() {
        super.removed();
        itemButtons.clear();
    }
}
