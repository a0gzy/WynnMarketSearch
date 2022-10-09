package me.a0g.wms.core

import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import java.io.File

object Config : Vigilant(File("./config/Wms.toml")){

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom search",
        description = "Enables extended market search.",
        category = "Main"
    )
    var marketSearch = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto focus",
        description = "Automaticly focus the search field.",
        category = "Main"
    )
    var autoFocus = true

    init {
        initialize()
    }
}