package me.a0g.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Виджет текстового ввода (аналог TextInputWidget из WynnExtras)
 * Обертка над Minecraft TextFieldWidget с правильной обработкой фокуса
 */
public class SearchInputWidget {
    private final TextFieldWidget textField;
    private final Consumer<String> onChanged;
    private boolean focused = false;
    private boolean visible = true;
    private final double scaleFactor;

    public SearchInputWidget(int x, int y, int width, int height, Consumer<String> onChanged, double scaleFactor) {
        MinecraftClient client = MinecraftClient.getInstance();

        this.textField = new TextFieldWidget(
                client.textRenderer,
                x, y, width, height,
                Text.literal("Search")
        );
        this.textField.setPlaceholder(Text.literal("Item name"));
        this.textField.setChangedListener(text -> {
            if (onChanged != null) {
                onChanged.accept(text);
            }
        });
        this.textField.setMaxLength(100);
        this.onChanged = onChanged;
        this.scaleFactor = scaleFactor;
    }

    public void setBounds(int x, int y, int width, int height) {
        textField.setX(x);
        textField.setY(y);
    }

    public int getX() { return textField.getX(); }
    public int getY() { return textField.getY(); }
    public int getWidth() { return textField.getWidth(); }
    public int getHeight() { return textField.getHeight(); }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return textField.isMouseOver(mouseX, mouseY);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        textField.setVisible(visible);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            textField.setFocused(true);
            textField.setText(""); // Очищаем при фокусе
        } else {
            textField.setFocused(false);
        }
    }

    public boolean isFocused() {
        return focused && textField.isFocused();
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public boolean isEmpty() {
        return textField.getText().isEmpty();
    }

    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        textField.render(context, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, boolean doubleClick) {
        if (!visible) return false;

        boolean clicked = textField.isMouseOver(mouseX, mouseY);
        if (clicked) {
            focused = true;
            textField.setFocused(true);
        }
        return clicked;
    }

    public boolean keyPressed(KeyInput input) {
        if (!visible) return false;
        return textField.keyPressed(input);
    }

    public boolean charTyped(CharInput input) {
        if (!visible) return false;
        return textField.charTyped(input);
    }

    public TextFieldWidget getTextField() {
        return textField;
    }
}
