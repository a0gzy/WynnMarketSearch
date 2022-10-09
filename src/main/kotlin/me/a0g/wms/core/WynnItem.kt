package me.a0g.wms.core

import com.google.gson.JsonObject
import gg.essential.universal.ChatColor
import me.a0g.wms.Wms
import net.minecraft.client.Minecraft
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import scala.annotation.switch

class WynnItem(var json: JsonObject) {
    var name = json["name"].asString
//    var type = json.get("type").asString//["type"].asString
//    var armorType = json.get("armorType").asString//["armorType"].asString
//    var material = json.get("material").asString//["material"].asString

    fun getTier():String{
        val tier:String? = if(json.has("tier") && !json.get("tier").isJsonNull) json.get("tier").asString.lowercase() else null
        var color:String = ""
        when(tier) {
            "normal" -> color = ChatColor.GRAY.toString()
            "unique" -> color = ChatColor.YELLOW.toString()
            "rare" -> color = ChatColor.LIGHT_PURPLE.toString()
            "legendary" -> color = ChatColor.AQUA.toString()
            "fabled" -> color = ChatColor.RED.toString()
            "mythic" -> color = ChatColor.DARK_PURPLE.toString()
            "set" -> color = ChatColor.GREEN.toString()
        }

        return color
    }

    fun getNameToChat():String{
        val name = this.name
        if(name.isNullOrEmpty())
            return ""

        return name
    }

    fun getTextToRender():String{
        val color = getTier()
        val text = name;
        if(text.isNullOrEmpty()) return ""

        return "$color$text"
    }

    fun getItemToRender():ItemStack? {
       // Wms.logger.info(json)
        val type:String? = if(json.has("type") && !json.get("type").isJsonNull) json.get("type").asString else null
        val armorType:String? = if(json.has("armorType") && !json.get("armorType").isJsonNull) json.get("armorType").asString else null
        val material:String? = if(json.has("material") && !json.get("material").isJsonNull) json.get("material").asString else null

        var itemStack:ItemStack? = null
        if(material.isNullOrEmpty()){
            if(type != null && armorType != null){
                val item =  Item.getByNameOrId("minecraft:${armorType.lowercase().replace("chain".toRegex(),"chainmail")}_${type.lowercase()}")
                itemStack = item?.let { ItemStack(it) }
            }
        }
        else{
            val id = material.replace(":.+".toRegex(),"").toInt()
            val metadata = material.replace(".+:".toRegex(),"").toInt()
            //Wms.logger.info(metadata)
            val item = Item.getItemById(id)
            if(metadata <= 0){
                itemStack = ItemStack(item)
            }
            else{
                val iStack = ItemStack(item)
                val tag = NBTTagCompound()
                tag.setBoolean("Unbreakable",true)
                iStack.tagCompound = tag
                iStack.itemDamage = metadata
                itemStack = iStack
            }
        }

        if(itemStack != null)
            return itemStack

        return null
    }
}