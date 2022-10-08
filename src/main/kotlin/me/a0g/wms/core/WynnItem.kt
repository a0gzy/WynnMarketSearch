package me.a0g.wms.core

import com.google.gson.JsonObject
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
                /*when(type.lowercase()) {
                    "chestplate" -> when(armorType.lowercase()) {
                        "diamond" -> id = 311
                        "golden" -> id = 315
                        "iron" -> id = 307
                        "chain" ->
                        "leather" ->

                    }
                    "leggings" -> when(armorType) {

                    }
                }*/
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
                //iStack.set
                //iStack.metadata.plus(metadata)
                itemStack = iStack
                //Wms.logger.info(iStack.metadata. + " | " + metadata)
            }
            //item.set
            //item = Item.getItemById(material.replace(":.+".toRegex(),"").toInt())
        }
        //var item = Item.getItemById(2)
        if(itemStack != null)
            return itemStack

        return null
    }
}