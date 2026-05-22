package de.pocketcloud.cloud.plugin.loader;

import de.pocketcloud.cloud.plugin.CloudPlugin;
import de.pocketcloud.cloud.plugin.CloudPluginClassLoader;
import de.pocketcloud.cloud.plugin.CloudPluginDescription;
import de.pocketcloud.cloud.plugin.exception.PluginLoadFailedException;
import de.pocketcloud.cloud.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarCloudPluginLoader {

    public boolean canLoad(Path path) {
        if (Files.isRegularFile(path)) {
            if (FileUtils.extensionOf(path).equals("jar")) {
                try (JarFile jarFile = new JarFile(path.toFile())) {
                    JarEntry entry = jarFile.getJarEntry("plugin.yml");
                    if (entry == null) throw new PluginLoadFailedException("No plugin.yml found in " + path);
                    return true;
                } catch (IOException e) {
                    throw new PluginLoadFailedException(e);
                }
            }
        }
        return false;
    }

    public CloudPlugin load(Path path) throws PluginLoadFailedException {
        File pluginJar = path.toFile();
        CloudPluginClassLoader classLoader = null;
        try (JarFile jarFile = new JarFile(pluginJar)) {
            JarEntry entry = jarFile.getJarEntry("plugin.yml");
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                CloudPluginDescription description = FileUtils.YAML.loadAs(inputStream, CloudPluginDescription.class);
                if (description.getName() == null) throw new PluginLoadFailedException("No plugin name found in plugin.yml: " + path);
                if (description.getVersion() == null) throw new PluginLoadFailedException("No plugin version found in plugin.yml: " + path);
                if (description.getMain() == null) throw new PluginLoadFailedException("No plugin main found in plugin.yml: " + path);

                classLoader = new CloudPluginClassLoader(this.getClass().getClassLoader(), pluginJar);
                Class<?> mainClass = classLoader.loadClass(description.getMain());
                if (!CloudPlugin.class.isAssignableFrom(mainClass)) throw new PluginLoadFailedException("Main class does not extend CloudPlugin");

                Path dataFolder = Paths.get("storage/plugins/", description.getName());
                if (!Files.exists(dataFolder)) Files.createDirectories(dataFolder);

                Class<? extends CloudPlugin> castedMain = mainClass.asSubclass(CloudPlugin.class);
                CloudPlugin main = castedMain.getDeclaredConstructor().newInstance();
                main.init(classLoader, description, dataFolder, path);
                return main;
            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            try {
                if (classLoader != null) classLoader.close();
            } catch (IOException ex) {
                throw new PluginLoadFailedException(ex);
            }

            throw new PluginLoadFailedException(e);
        }
    }
}