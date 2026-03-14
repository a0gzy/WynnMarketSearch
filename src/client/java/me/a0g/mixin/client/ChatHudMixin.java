package me.a0g.mixin.client;

import me.a0g.Wms;
import me.a0g.config.ModConfig;
import me.a0g.gui.SearchGuiScreen;
import me.a0g.WmsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, CallbackInfo ci) {
        if (!ModConfig.get().marketSearch) {
            return;
        }

        String content = message.getString();

        if (content.contains("Type the item name") || content.contains("'cancel' to cancel")) {
            Wms.LOGGER.info("Market search message detected in ChatHud!");

            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (WmsClient.getWynnApi() != null) {
                    SearchGuiScreen screen = new SearchGuiScreen(WmsClient.getWynnApi());
                    client.setScreen(screen);
                }
            });
        }
    }
}
