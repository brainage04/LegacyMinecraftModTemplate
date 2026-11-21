package com.example.init;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.AnnotationVisitor;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.ClassVisitor;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Registers every class in the mixin package as a mixin, so the mixin configuration lists none by
 * hand.
 *
 * <p>Mixin calls {@link #getMixins()} once per config (MixinConfig.postInitialise, mixin 0.7.11).
 * Classes in the {@code client} sub-package are returned only when the environment's side is {@link
 * MixinEnvironment.Side#CLIENT}, so a dedicated server never loads them: a client mixin names
 * client-only classes, and loading it there fails the server at startup. The side comes from the
 * launch's tweak class, or from Forge's launch handler when no tweak class matches.
 *
 * <p>Adapted from lineargraph/Forge1.8.9Template 09cd2809.
 */
public class AutoDiscoveryMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("AutoDiscoveryMixinPlugin");
    private static final String CLIENT_PACKAGE = "client.";
    private static final String MIXIN_DESCRIPTOR = "Lorg/spongepowered/asm/mixin/Mixin;";

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
        LOGGER.info(
                "[AutoDiscoveryMixinPlugin] side {}, mixins {}",
                MixinEnvironment.getCurrentEnvironment().getSide(),
                mixins);
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
            String mixinRoot = mixinPackage.replace(".", "/") + "/";
            boolean packagePresent = false;
            try (ZipInputStream zis =
                    new ZipInputStream(Files.newInputStream(Paths.get(URI.create(jarPath))))) {
                ZipEntry next;
                while ((next = zis.getNextEntry()) != null) {
                    // Read only the mixin package's classes: a shadowed dependency puts tens of
                    // megabytes of other classes in the same jar.
                    if (next.getName().startsWith(mixinRoot)) {
                        packagePresent = true;
                        addMixin(next.getName(), readAll(zis), found);
                    }
                    zis.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!packagePresent) {
                throw new IllegalStateException(
                        "Cannot find the mixin package " + mixinPackage + " inside " + jarPath);
            }
            return;
        }

        Path directory;
        try {
            directory = Paths.get(URI.create(classRoot));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Cannot read the class root " + classRoot, e);
        }
        Path mixinDirectory = directory.resolve(mixinPackage.replace(".", "/"));
        if (!Files.isDirectory(mixinDirectory)) {
            throw new IllegalStateException(
                    "Cannot find the mixin package " + mixinPackage + " under " + directory);
        }
        try (Stream<Path> classes = Files.walk(mixinDirectory)) {
            classes.filter(Files::isRegularFile)
                    .forEach(
                            it -> {
                                try {
                                    addMixin(
                                            directory.relativize(it).toString(),
                                            Files.readAllBytes(it),
                                            found);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * Adds a class file to {@code found} as a name relative to the mixin package, if it is inside
     * that package and carries {@code @Mixin}.
     *
     * <p>The annotation decides, not the name: an anonymous or nested class of a mixin is part of
     * that mixin and Mixin rejects it ("is missing an @Mixin annotation",
     * MixinInfo.readDeclaredTargets), which fails the whole config because it is {@code required}.
     * A static nested class that does carry {@code @Mixin} is a mixin of its own and Mixin applies
     * it, so skipping every name with a {@code $} would silently drop it.
     */
    private void addMixin(String classFile, byte[] bytes, List<String> found) {
        if (!classFile.endsWith(".class")) {
            return;
        }
        String name =
                classFile
                        .substring(0, classFile.length() - ".class".length())
                        .replace("\\", "/")
                        .replace("/", ".");
        String prefix = mixinPackage + ".";
        if (!name.startsWith(prefix) || name.endsWith(".") || !isMixin(bytes)) {
            return;
        }
        found.add(name.substring(prefix.length()));
    }

    private static boolean isMixin(byte[] bytes) {
        boolean[] annotated = new boolean[1];
        new ClassReader(bytes)
                .accept(
                        new ClassVisitor(Opcodes.ASM5) {
                            @Override
                            public AnnotationVisitor visitAnnotation(
                                    String descriptor, boolean visible) {
                                if (MIXIN_DESCRIPTOR.equals(descriptor)) {
                                    annotated[0] = true;
                                }
                                return null;
                            }
                        },
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return annotated[0];
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
