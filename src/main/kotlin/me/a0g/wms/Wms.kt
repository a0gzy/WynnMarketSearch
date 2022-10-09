package me.a0g.wms


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.essential.api.EssentialAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.a0g.wms.commands.WmsCommand
import me.a0g.wms.core.Config
import me.a0g.wms.core.UpdateChecker
import me.a0g.wms.core.WynnItem
import me.a0g.wms.gui.SearchGui
import net.minecraft.client.gui.GuiMainMenu
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
//import okhttp3.OkHttpClient
//import okhttp3.Request
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.net.URL

@Mod(
    modid = Wms.MODID,
    version = Wms.VERSION,
    name = Wms.NAME,
    clientSideOnly = true,
    modLanguageAdapter = "gg.essential.api.utils.KotlinAdapter"
)
object Wms {
    const val MODID = "wms"
    const val VERSION = "0.0.3"
    const val NAME = "WynnMarketSearch"

    val logger: Logger = LogManager.getLogger()

    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //val okHttpClient = OkHttpClient()
    val jsonParser = JsonParser()
    private val mutex: Mutex = Mutex()

    var wynnData: JsonObject = JsonObject()

    lateinit var config: Config

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        scope.launch {
            //getWynnItems()
            getData()
        }.invokeOnCompletion {
            if (it == null)
                logger.info("WynnData here")
        }
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        config = Config
    }

    @Mod.EventHandler
    fun postInit(ignored: FMLPostInitializationEvent) {

        arrayOf(
            this,
            UpdateChecker()
        ).forEach(MinecraftForge.EVENT_BUS::register)

        EssentialAPI.getCommandRegistry().registerCommand(WmsCommand())
    }

    @SubscribeEvent
    fun onChatMessage(event: ClientChatReceivedEvent){
        if(event.message.formattedText.contains("Type the item name or type 'cancel' to cancel:") && config.marketSearch){
            EssentialAPI.getGuiUtil().openScreen(SearchGui())
        }
    }


    suspend fun getData(){
        val text = URL("https://api.wynncraft.com/public_api.php?action=itemDB&category=all").readText()
        mutex.withLock(wynnData) {
            wynnData = jsonParser.parse(text).asJsonObject
        }
    }

    fun getItems():MutableList<WynnItem>{
        val items: JsonArray =wynnData.get("items").asJsonArray
        val wynnItems = mutableListOf<WynnItem>()

        for(item in items) {
            wynnItems.add(WynnItem(item.asJsonObject))
        }

        return wynnItems
    }


    fun check(itemName: String,text:String): Boolean{
        val rItemName = itemName.replace("-".toRegex()," ")
        val splitText = text.replace("-".toRegex()," ").split(" ")
        var rText:String = ""
        for(split in splitText){
            rText += "($split)*"
        }
        if(rItemName.contains(rText.toRegex()))
            return true

        //Item - Morph-Topaz | text - topaz morph
        //morph topaz | topaz morph
        /*if(rItemName.contains(rText.toRegex()))
            return true*/
        return false
    }

    //var item = WynnItem(wynnData.get("items").asJsonArray.get(0).asJsonObject)
    // https://api.wynncraft.com/public_api.php?action=itemDB&category=all
}