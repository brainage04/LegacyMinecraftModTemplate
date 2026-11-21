package com.example.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mixin that applies on both sides, so a dedicated server has one too.
 *
 * <p>It reads {@code tickCounter}, which is private in vanilla and made public by {@code
 * accesstransformer.cfg}; without that entry this class does not compile.
 *
 * <p>The constructor is named with its descriptor because 1.8.9 declares two of them and a bare
 * {@code <init>} selector matches both, so the injection would run twice; the descriptor picks the
 * one {@code DedicatedServer} calls.
 */
@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Inject(method = "<init>(Ljava/io/File;Ljava/net/Proxy;Ljava/io/File;)V", at = @At("RETURN"))
    private void examplemod$logConstruction(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        System.out.println("Server mixin applied; tick counter " + server.tickCounter);
    }
}
