package me.a0g.wms.gui.components

import gg.essential.elementa.UIComponent
import gg.essential.universal.UMatrixStack
import me.a0g.wms.Wms
import me.a0g.wms.core.WynnItem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.ItemStack

class ItemRenderComponent(var item: WynnItem?) : UIComponent(){

    init {

    }

    override fun draw(matrixStack: UMatrixStack) {

        beforeDraw(matrixStack)
        super.draw(matrixStack)

        if(item != null) {
            val x = this.getLeft()
            val y = this.getTop()
            val scale:Float = 1.2F

            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, scale);

            val itemStack = item?.getItemToRender()
            //Wms.logger.info(itemStack?.metadata)

            if(itemStack != null){
                renderItem(itemStack,x / scale,y / scale)
            }


            GlStateManager.popMatrix();
        }

        //RenderUtil.renderArmor(armor, x / scale.toFloat(), y / scale.toFloat() + offset);

        //GlStateManager.popMatrix();
    }

    fun renderItem(item: ItemStack?, x: Float, y: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.enableRescaleNormal()
        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.translate(x, y, -100f)
        var font: FontRenderer? = null
        if (item != null) {
            font = item.item.getFontRenderer(item)
        }
        if (font == null) {
            font = Minecraft.getMinecraft().fontRenderer
        }
        Minecraft.getMinecraft().renderItem.renderItemIntoGUI(item, 0, 0)
        Minecraft.getMinecraft().renderItem.renderItemOverlayIntoGUI(font, item, 0, 0, "")
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.popMatrix()
    }
}