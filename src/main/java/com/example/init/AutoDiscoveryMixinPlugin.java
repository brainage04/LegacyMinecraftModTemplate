package com.example.init;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Registers every class in the mixin package as a mixin, so the mixin configuration lists none by
 * hand.
 *
 * <p>Mixin calls {@link #getMixins()} once per config, after the environment's side is known
 * (MixinConfig.postInitialise, mixin 0.7.11). Classes in the {@code client} sub-package are
 * returned only when that side is {@link MixinEnvironment.Side#CLIENT}, so a dedicated server never
 * loads them: a client mixin names client-only classes, and loading it there fails the server at
 * startup.
 *
 * <p>Adapted from lineargraph/Forge1.8.9Template 09cd2809.
 */
public class AutoDiscoveryMixinPlugin implements IMixinConfigPlugin {
    private static final String CLIENT_PACKAGE = "client.";

    private String mixinPackage;
    private List<String> mixins;

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage;
    }

    @Override
    public List<String> getMixins() {
        if (mixins != null) {
            return mixins;
        }

        List<String> found = new ArrayList<>();
        collect(found);

        boolean client =
                MixinEnvironment.getCurrentEnvironment().getSide() == MixinEnvironment.Side.CLIENT;
        List<String> selected = new ArrayList<>();
        for (String mixin : found) {
            if (client || !mixin.startsWith(CLIENT_PACKAGE)) {
                selected.add(mixin);
            }
        }

        mixins = Collections.unmodifiableList(selected);
        System.out.println(
                "[AutoDiscoveryMixinPlugin] side "
                        + MixinEnvironment.getCurrentEnvironment().getSide()
                        + ", mixins "
                        + mixins);
        return mixins;
    }

    /**
     * Adds every class of the mixin package to {@code found}.
     *
     * <p>The search starts from this class's own resource, not from its code source: a loader may
     * report a code source that is not where the classes are, and it need not serve directory
     * resources at all. This class sits in the same class root as the mixins, so its resource URL
     * gives that root in a development run (a directory) and in a built jar alike.
     */
    private void collect(List<String> found) {
        String ownResource = getClass().getSimpleName() + ".class";
        URL own = getClass().getResource(ownResource);
        if (own == null) {
            throw new IllegalStateException("Cannot locate " + ownResource + " to find the mixins");
        }

        String ownPath = getClass().getName().replace(".", "/") + ".class";
        String url = own.toString();
        if (!url.endsWith(ownPath)) {
            throw new IllegalStateException("Unexpected location for " + ownResource + ": " + url);
        }
        String classRoot = url.substring(0, url.length() - ownPath.length());

        if (classRoot.startsWith("jar:")) {
            String jarPath = classRoot.substring(4).split("!")[0];
            try (ZipInputStream zis =
                    new ZipInputStream(Files.newInputStream(Paths.get(URI.create(jarPath))))) {
                ZipEntry next;
                while ((next = zis.getNextEntry()) != null) {
                    addMixin(next.getName(), found);
                    zis.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        Path directory;
        try {
            directory = Paths.get(URI.create(classRoot));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Cannot read the class root " + classRoot, e);
        }
        Path mixinRoot = directory.resolve(mixinPackage.replace(".", "/"));
        if (!Files.isDirectory(mixinRoot)) {
            return;
        }
        try (Stream<Path> classes = Files.walk(mixinRoot)) {
            classes.filter(Files::isRegularFile)
                    .map(it -> directory.relativize(it).toString())
                    .forEach(it -> addMixin(it, found));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a class file path to {@code found} as a name relative to the mixin package, if it is
     * inside it.
     */
    private void addMixin(String classFile, List<String> found) {
        String name =
                (classFile.endsWith(".class")
                                ? classFile.substring(0, classFile.length() - ".class".length())
                                : classFile)
                        .replace("\\", "/")
                        .replace("/", ".");
        String prefix = mixinPackage + ".";
        if (name.startsWith(prefix) && !name.endsWith(".")) {
            found.add(name.substring(prefix.length()));
        }
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
}
