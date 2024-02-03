package com.example.keybinds

import com.example.ExampleMod
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard

class ConfigKeyBind : KeyBinding("Open Config GUI", Keyboard.KEY_RETURN, ExampleMod.MOD_NAME) {
    @SubscribeEvent
    fun tick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return

        if (this.isPressed) ExampleMod.configManager.openConfigGui()
    }
}