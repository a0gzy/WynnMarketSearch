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

    fun getLastVersion(){
        Wms.logger.info("getLastVersion")
        val text = URL("https://api.github.com/repos/a0gzy/WynnMarketSearch/releases/latest").readText()
        val gitData = Wms.jsonParser.parse(text).asJsonObject
        val newVersion = gitData["tag_name"].asString

        val versionSplit = Wms.VERSION.split(".")
        val newVersionSplit = newVersion.split(".")
        Wms.logger.info(versionSplit)
        Wms.logger.info(newVersionSplit)

        for(i in 0 until 3){
            Wms.logger.info(i)
            if(newVersionSplit[i].toInt() > versionSplit[i].toInt()){
                this.isUpdatedForPush = true
                this.updateBody = gitData["body"].asString
                Wms.logger.info("new update")

                return
            }
        }

    }

    @SubscribeEvent
    fun onGuiOpen(e: GuiScreenEvent.DrawScreenEvent.Post) {
        if (e.gui is GuiMainMenu) {
            if (this.isUpdatedForPush) {
                Wms.logger.info("Wms update founded")
                EssentialAPI.getNotifications().pushWithDurationAndAction(
                    title = "New WynnMarketSearch version",
                    message = this.updateBody,
                    duration = 15f
                ) { openGitSite() }
                this.isUpdatedForPush = false
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