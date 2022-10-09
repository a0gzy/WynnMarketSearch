package me.a0g.wms.commands

import gg.essential.api.EssentialAPI
import gg.essential.api.commands.Command
import gg.essential.api.commands.DefaultHandler
import gg.essential.api.commands.DisplayName
import gg.essential.api.commands.SubCommand
import gg.essential.universal.ChatColor
import kotlinx.coroutines.launch
import me.a0g.wms.Wms
import me.a0g.wms.gui.SearchGui
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound


class WmsCommand: Command("wms") {

    @DefaultHandler
    fun handle() {
        EssentialAPI.getGuiUtil().openScreen(Wms.config.gui())
    }


    @SubCommand(value = "refresh", description = "refresh items from api")
    fun handleRefresh() {
        Wms.scope.launch {
            launch {
                Wms.getData()
               // Wms.getWynnItems()
            }
        }.invokeOnCompletion {
            if (it == null){
                EssentialAPI.getMinecraftUtil().sendMessage("${ChatColor.GREEN}Refreshed!")
                Wms.logger.info(Wms.wynnData)
            }
            else
                EssentialAPI.getMinecraftUtil().sendMessage("${ChatColor.RED}Refresh failed!")
        }
    }

    @SubCommand(value = "data", description = "send item nbt to console")
    fun handleData() {
        try {
            val currentItem = Minecraft.getMinecraft().player.inventory.getCurrentItem()
            val nbtData = currentItem.tagCompound.toString()
            EssentialAPI.getMinecraftUtil().sendMessage(nbtData)
        } catch (e: Exception) {e.printStackTrace()}
//        Wms.logger.info(currentItem.itemDamage )
        //Wms.logger.info(Wms.wynnData)
    }

    /*@SubCommand(value = "fdata")
    fun handleFData() {
        val currentItem = Minecraft.getMinecraft().player.inventory.getCurrentItem()
        val tag = NBTTagCompound()
        tag.setBoolean("Unbreakable",true)
        currentItem.tagCompound = tag
        currentItem.itemDamage = 2
    }*/

    @SubCommand(value = "gui",description = "Display gui for testing purposes")
    fun handleGui() {
        EssentialAPI.getGuiUtil().openScreen(SearchGui())
    }

}