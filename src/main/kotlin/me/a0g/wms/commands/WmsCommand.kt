package me.a0g.wms.commands

import gg.essential.api.EssentialAPI
import gg.essential.api.commands.Command
import gg.essential.api.commands.DefaultHandler
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

    @SubCommand(value = "refresh")
    fun handleRefresh() {
        Wms.scope.launch {
            launch {
                Wms.getWynnItems()
            }
        }.invokeOnCompletion {
            if (it == null)
                EssentialAPI.getMinecraftUtil().sendMessage("${ChatColor.GREEN}Refreshed!")
            else
                EssentialAPI.getMinecraftUtil().sendMessage("${ChatColor.RED}Refresh failed!")
        }
    }

    @SubCommand(value = "data")
    fun handleData() {
        val currentItem = Minecraft.getMinecraft().player.inventory.getCurrentItem()
        val data = currentItem.tagCompound.toString()
        EssentialAPI.getMinecraftUtil().sendMessage(currentItem.itemDamage.toString())
//        Wms.logger.info(currentItem.itemDamage )
        //Wms.logger.info(Wms.wynnData)
    }

    @SubCommand(value = "fdata")
    fun handleFData() {
        val currentItem = Minecraft.getMinecraft().player.inventory.getCurrentItem()
        val tag = NBTTagCompound()
        tag.setBoolean("Unbreakable",true)
        currentItem.tagCompound = tag
        currentItem.itemDamage = 2
        //EssentialAPI.getMinecraftUtil().sendMessage(currentItem.metadata.toString() + " " )
        //currentItem.metadata

        /*val items: JsonArray = Wms.wynnData.get("items").asJsonArray
        val itemNamesList = mutableListOf<String>()

        for(item in items) {
           itemNamesList.add(item.asJsonObject.get("name").asString)
        }

        Wms.logger.info(itemNamesList)*/
    }

    @SubCommand(value = "gui")
    fun handleGui() {
        EssentialAPI.getGuiUtil().openScreen(SearchGui())
    }

}