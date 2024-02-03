package com.example.utils

import com.example.ExampleMod
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText

object ChatUtils {
    // character codes
    const val darkRedChar = "§4"
    const val redChar = "§c"
    const val goldChar = "§6"
    const val yellowChar = "§e"
    const val darkGreenChar = "§2"
    const val greenChar = "§a"
    const val aquaChar = "§b"
    const val darkAquaChar = "§3"
    const val darkBlueChar = "§1"
    const val blueChar = "§9"
    const val lightPurpleChar = "§d"
    const val darkPurpleChar = "§5"
    const val whiteChar = "§f"
    const val grayChar = "§7"
    const val darkGrayChar = "§8"
    const val blackChar = "§0"

    const val obfuscatedChar = "§k"
    const val boldChar = "§l"
    const val strikethroughChar = "§m"
    const val underlineChar = "§n"
    const val italicChar = "§o"

    const val resetChar = "§r"

    @JvmStatic
    fun messageToChat(message: String, soundType: SoundUtils.SoundType = SoundUtils.SoundType.NONE) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(ChatComponentText("[${ExampleMod.MOD_NAME}] $message"))

        when (soundType) {
            SoundUtils.SoundType.NONE -> {}
            SoundUtils.SoundType.NOTIFICATION -> SoundUtils.playNotificationSound()
            SoundUtils.SoundType.ALERT -> SoundUtils.playAlertSound()
        }
    }
}