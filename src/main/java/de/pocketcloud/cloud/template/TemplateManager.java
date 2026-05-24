package de.pocketcloud.cloud.template;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.template.TemplateCreateEvent;
import de.pocketcloud.cloud.event.impl.template.TemplateEditEvent;
import de.pocketcloud.cloud.event.impl.template.TemplateRemoveEvent;
import de.pocketcloud.cloud.load.Loadable;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.template.group.ServerGroupManager;
import de.pocketcloud.cloud.template.util.TemplateEditData;
import de.pocketcloud.cloud.tick.Tickable;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.Utils;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTiming;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TemplateManager implements Tickable, Loadable {

    @Getter
    private static TemplateManager instance = null;

    private final Map<String, Template> templates = new HashMap<>();

    public TemplateManager() {
        instance = this;
    }

    //TODO broadcast sync packets

    @Override
    public void load() {
        CloudLogger.get().info("Loading templates...");
        for (TemplateType type : TemplateType.values()) FileUtils.createDir(type.globalTemplatePath());
        CloudProvider.current().getTemplates()
                .thenAccept(this.templates::putAll)
                .thenAccept(_ -> ServerGroupManager.getInstance().load());
    }

    @Override
    public void unload() {
        CloudLogger.get().info("Unloading templates...");
        this.templates.clear();
        ServerGroupManager.getInstance().unload();
    }

    public void create(Template template) {
        Benchmark.startTiming("template_creation");
        try {
            TemplateCreateEvent ev = new TemplateCreateEvent(template);
            ev.call();
            if (ev.isCancelled()) {
                Benchmark.stopTiming("template_creation");
                return;
            }

            CloudProvider.current().addTemplate(template);
            if (!Files.isDirectory(template.path())) Files.createDirectories(template.path());
            templates.put(template.name(), template);
            BenchmarkTiming res = Benchmark.stopTiming("template_creation");
            CloudLogger.get().success("Successfully §acreated §rthe template §b{}§r. §8(§rTook §b{}ms§8)", template.name(), Utils.formatNumber(res.duration(), 2));
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to create template {}", e, template.name());
            Benchmark.stopTiming("template_creation");
            templates.remove(template.name());
        }
    }

    public void edit(Template template, TemplateEditData editData) {
        Benchmark.startTiming("template_editing");
        TemplateEditEvent ev = new TemplateEditEvent(template, editData);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("template_editing");
            return;
        }

        editData.applyTo(template);
        CloudProvider.current().editTemplate(template, template.write());
        BenchmarkTiming res = Benchmark.stopTiming("template_editing");
        CloudLogger.get().success("Successfully §eedited §rthe template §b{}§r. §8(§rTook §b{}ms§8)", template.name(), Utils.formatNumber(res.duration(), 2));
        //TODO kick playxers from template if new edit data has maintenance=true
    }

    public void remove(Template template) {
        Benchmark.startTiming("template_removal");
        TemplateRemoveEvent ev = new TemplateRemoveEvent(template);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("template_removal");
            return;
        }

        CloudProvider.current().removeTemplate(template);
        if (Files.isDirectory(template.path())) {
            try {
                FileUtils.removeDirectory(template.path());
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to remove template directory of {}", e, template.name());
            }
        }

        templates.remove(template.name());
        BenchmarkTiming res = Benchmark.stopTiming("template_removal");
        CloudLogger.get().success("Successfully §cremoved §rthe template §b{}§r. §8(§rTook §b{}ms§8)", template.name(), Utils.formatNumber(res.duration(), 2));
    }

    public boolean check(String name) {
        return templates.containsKey(name);
    }

    @Override
    public void tick(long currentTick) {
        //start servers
    }

    public Optional<Template> get(String name) {
        return Optional.ofNullable(templates.getOrDefault(name, null));
    }

    public List<Template> getAll(TemplateType... types) {
        if (types.length > 0) return templates.values().stream()
                .filter(template -> {
                    for (TemplateType type : types) {
                        if (template.isTypeOf(type)) return true;
                    }
                    return false;
                }).toList();
        return templates.values().stream().toList();
    }
}