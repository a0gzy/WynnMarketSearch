package me.a0g.wms.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.FillConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.a0g.wms.Wms
import me.a0g.wms.core.WynnItem
import me.a0g.wms.gui.components.ItemContainer
import net.minecraft.client.Minecraft
import org.lwjgl.input.Keyboard
import java.awt.Color

class SearchGui : WindowScreen(ElementaVersion.V2) {

    var toCancel = true

    private val container = UIContainer()

    private val searchText = UITextInput(placeholder = "Item name")

    private var itemList = mutableListOf<ItemContainer>()

    init {

        container.constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 400.pixels
            height = 250.pixels
        } childOf window effect OutlineEffect(Color.GRAY,2f)

        val searchTextContainer = UIBlock(Color.BLACK).constrain {
            x = 2.pixels
            y = 2.pixels
            width = 400.pixels - 4.pixels
            height = 30.pixels
        } childOf container effect OutlineEffect(Color.DARK_GRAY,2f)

        searchText.constrain {
            x = 5.pixels
            y = 11.pixels
            height = FillConstraint()
            width = FillConstraint()
            textScale = 1.8f.pixels
        } childOf searchTextContainer

        searchTextContainer.onMouseClick { searchText.grabWindowFocus() }

        if(Wms.config.autoFocus) {
            searchText.grabWindowFocus()
        }

        for(i in 0 until 7){
            itemList.add(ItemContainer(null))
        }

        searchText.onKeyType { typedChar, keyCode ->

            for(i in 0 until 7){
                itemList[i].setWynn(null)
                //itemList[i].setText("")
               // itemList[i].setWynnItem(null)
            }

            val text = searchText.getText().lowercase()
            if(keyCode == Keyboard.KEY_RETURN){
                if(text.isNotEmpty())
                    sendToChat(text)
            }



            val items = Wms.getItems()
            val correctItems =  mutableListOf<WynnItem>()


            if(text.isNotEmpty()) {
                for(item in items) {
                    val itemName = item.name.lowercase()
                    val splitItemName = itemName.replace("-".toRegex()," ").split(" ".toRegex())
                    val splitText = itemName.replace("-".toRegex()," ").split(" ".toRegex())

                    if(itemName.contains(text) || itemName.replace("-".toRegex()," ").contains(text) ){
                        correctItems.add(item)
                    }
                    /*if(Wms.check(itemName,text)){
                        correctItems.add(item)
                    }*/

                }

                /*try {
                    Wms.logger.info(correctItems[0].name)
                }catch(e: Exception) {e.printStackTrace()}*/

                for(i in itemList.indices){
                    if(correctItems.isNotEmpty() && i < correctItems.size && correctItems[i] != null){
                        itemList[i].setWynn(correctItems[i])
                        //itemList[i].setWynnItem(correctItems[i])
                        //itemList[i].setText(correctItems[i].name)
                    }

                }
            }

        }


        val listItemsContainer = UIBlock(Color.BLACK).constrain {
            x = 0.pixels
            y = 34.pixels
            width = 400.pixels
            height = FillConstraint() - 4.pixels
        } childOf container

        val itemListLine = UIBlock(Color.GRAY).constrain {
            x = 0.pixels
            y = 0.pixels
            width = 400.pixels
            height = 2.pixels
        } childOf listItemsContainer

        for(i in itemList.indices){
            itemList[i].constrain {
                x = 2.pixels
                y = 4.pixels + (i * 30).pixels
                width = 400.pixels - 4.pixels
                height = 30.pixels
            } childOf listItemsContainer effect OutlineEffect(Color.DARK_GRAY,2f)
            itemList[i].onMouseClick {
                if(itemList[i].getText().isNotEmpty()) {

                    toCancel = false
                    Minecraft.getMinecraft().player.closeScreen()

                    Wms.scope.launch {
                        launch {
                            withContext(Dispatchers.IO) {
                                Thread.sleep(100)
                            }
                            Minecraft.getMinecraft().player.sendChatMessage(itemList[i].item!!.getNameToChat())
                           // Minecraft.getMinecraft().player.sendChatMessage(itemList[i].getText())
                           // Wms.getWynnItems()
                        }
                    }

                    //super.onScreenClose()
                }
            }
        }

    }

    fun sendToChat(text: String){
        toCancel = false
        Minecraft.getMinecraft().player.closeScreen()

        Wms.scope.launch {
            launch {
                withContext(Dispatchers.IO) {
                    Thread.sleep(100)
                }
                Minecraft.getMinecraft().player.sendChatMessage(text)
            }
        }
    }


    override fun onScreenClose() {
        if(toCancel)
            Minecraft.getMinecraft().player.sendChatMessage("cancel")

        super.onScreenClose()
    }
}