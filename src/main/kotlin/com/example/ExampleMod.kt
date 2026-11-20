package com.example

import com.example.config.ExampleModConfig
import com.example.config.manager.ConfigManager
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.LogManager

@Mod(modid = ExampleMod.MOD_ID, useMetadata = true)
class ExampleMod {
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        if (isClient) {
            ClientSetup.preInit()
        }
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        if (isClient) {
            ClientSetup.init()
        }
    }

    companion object {
        lateinit var configManager: ConfigManager
        const val MOD_ID = "examplemod"
        const val MOD_NAME = "ExampleMod"
        val LOGGER = LogManager.getLogger(MOD_ID)

        /**
         * True on a physical client. The client classes this mod uses (keybinds, the config GUI) are
         * absent from a dedicated server, so every use of them is behind this check.
         */
        val isClient: Boolean
            get() = FMLCommonHandler.instance().side.isClient

        @JvmStatic
        val version: String
            get() = Loader.instance().indexedModList[MOD_ID]!!.version

        val config: ExampleModConfig
            get() = configManager.config ?: error("config is null")
    }
}
