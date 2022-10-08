package me.a0g.wms.gui.components

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import me.a0g.wms.core.WynnItem

class ItemContainer(var item: WynnItem?) : UIContainer() {

    lateinit var text: UIText
    lateinit var itemStack:ItemRenderComponent

    init {
        text = UIText(if (item == null) "" else item!!.name)
        itemStack =  ItemRenderComponent(item)

        itemStack.constrain{
            x = 4.pixels
            y = 4.pixels
        } childOf this
        text.constrain {
            x = 40.pixels
            y = CenterConstraint()
        } childOf this
    }

    fun getText():String {
        return text.getText()
    }

    fun setText(name:String) {
        text.setText(name)
    }

    fun setWynnItem(wynnItem: WynnItem?){
        itemStack.item = wynnItem
    }

    /*fun setItem(item: WynnItem?){
        this.item = item
    }*/


}