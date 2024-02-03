package com.example.utils

import com.example.ExampleMod
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.PositionedSound
import net.minecraft.client.audio.SoundCategory
import net.minecraft.util.ResourceLocation

object SoundUtils {
    enum class SoundType {
        NONE,
        NOTIFICATION,
        ALERT,
    }

    private val alertSound by lazy { createSound("random.successful_hit", 1f, 1f) }
    private val notificationSound by lazy { createSound("gui.button.press", 1f, 1f) }

    private fun ISound.playSound() {
        Minecraft.getMinecraft().addScheduledTask {
            val gameSettings = Minecraft.getMinecraft().gameSettings
            val oldLevel = gameSettings.getSoundLevel(SoundCategory.PLAYERS)
            gameSettings.setSoundLevel(SoundCategory.PLAYERS, 1f)

            try {
                Minecraft.getMinecraft().soundHandler.playSound(this)
            } catch (e: Exception) {
                if (e is IllegalArgumentException) {
                    e.message?.let {
                        if (it.startsWith("value already present:")) {
                            ExampleMod.LOGGER.info("Sound error: $it")
                            return@addScheduledTask
                        }
                    }
                }

                e.printStackTrace()
            } finally {
                gameSettings.setSoundLevel(SoundCategory.PLAYERS, oldLevel)
            }
        }
    }

    private fun createSound(name: String, volume: Float, pitch: Float): ISound {
        val sound: ISound = object : PositionedSound(ResourceLocation(name)) {
            init {
                this.volume = volume
                this.pitch = pitch
                repeat = false
                repeatDelay = 0
                attenuationType = ISound.AttenuationType.NONE
            }
        }

        return sound
    }

    fun playNotificationSound() {
        notificationSound.playSound()
    }

    fun playAlertSound() {
        alertSound.playSound()
    }
}