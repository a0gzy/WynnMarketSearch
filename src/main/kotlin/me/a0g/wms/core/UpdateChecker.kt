package me.a0g.wms.core

import gg.essential.api.EssentialAPI
import me.a0g.wms.Wms
import net.minecraft.client.gui.GuiMainMenu
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Desktop
import java.net.URI
import java.net.URL

class UpdateChecker {

    var isUpdatedForPush = false
    var updateBody = ""

    suspend fun getLastVersion(){
        val text = URL("https://api.github.com/repos/a0gzy/WynnMarketSearch/releases/latest").readText()
        val gitData = Wms.jsonParser.parse(text).asJsonObject
        val newVersion = gitData["tag_name"].asString
        for(i in 0 until 2){
            val versionSplit = Wms.VERSION.split(".")
            val newVersionSplit = newVersion.split(".")

            if(newVersionSplit[i].toInt() > versionSplit[i].toInt()){
                isUpdatedForPush = true
                updateBody = gitData["body"].asString
            }
        }

    }

    @SubscribeEvent
    fun onGuiOpen(e: GuiScreenEvent.DrawScreenEvent.Post) {
        if (e.gui is GuiMainMenu) {
            if (isUpdatedForPush) {
                EssentialAPI.getNotifications().pushWithAction(
                    "New WynnMarketSearch version",
                    updateBody
                ) { openGitSite() }
                isUpdatedForPush = false
            }
        }
    }

    private fun openGitSite(){
        val desktop: Desktop? = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(URI("https://github.com/a0gzy/WynnMarketSearch/releases"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}