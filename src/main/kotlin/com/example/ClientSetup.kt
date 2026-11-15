package com.example

import com.example.commands.manager.CommandManager
import com.example.config.manager.ConfigManager
import com.example.keybinds.ConfigKeyBind
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry

/**
 * The client half of the mod: keybinds, the config GUI and client commands.
 *
 * Nothing here may be touched on a dedicated server, where the client classes it names do not
 * exist; ExampleMod calls it only when Forge reports the client side.
 */
object ClientSetup {
    private val configKeyBind = ConfigKeyBind()

    fun preInit() {
        CommandManager()

        ClientRegistry.registerKeyBinding(configKeyBind)
    }

    fun init() {
        ExampleMod.configManager = ConfigManager()
        MinecraftForge.EVENT_BUS.register(ExampleMod.configManager)
        MinecraftForge.EVENT_BUS.register(configKeyBind)
    }
}
