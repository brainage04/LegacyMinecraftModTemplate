package com.example.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mixin that applies on both sides, so a dedicated server has one too.
 *
 * <p>It reads {@code playersOnline}, which is private in vanilla and made public by {@code
 * accesstransformer.cfg}; without that entry this class does not compile.
 */
@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    private boolean examplemod$logged;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void examplemod$logConstruction(CallbackInfo ci) {
        if (examplemod$logged) {
            return;
        }
        examplemod$logged = true;
        MinecraftServer server = (MinecraftServer) (Object) this;
        System.out.println("Server mixin applied; players online: " + server.playersOnline.size());
    }
}
