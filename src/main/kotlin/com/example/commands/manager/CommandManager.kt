package com.example.commands.manager

import com.example.ExampleMod
import com.example.commands.core.SimpleCommand
import com.example.commands.core.SimpleCommand.ProcessCommandRunnable
import net.minecraft.command.ICommandSender
import net.minecraftforge.client.ClientCommandHandler

class CommandManager {
    init {
        registerCommand(ExampleMod.MOD_ID) {
            ExampleMod.configManager.openConfigGui()
        }
    }

    private fun registerCommand(name: String, function: (Array<String>) -> Unit) {
        ClientCommandHandler.instance.registerCommand(SimpleCommand(name, createCommand(function)))
    }

    private fun createCommand(function: (Array<String>) -> Unit) = object : ProcessCommandRunnable() {
        override fun processCommand(sender: ICommandSender?, args: Array<String>?) {
            if (args != null) function(args.asList().toTypedArray())
        }
    }
}