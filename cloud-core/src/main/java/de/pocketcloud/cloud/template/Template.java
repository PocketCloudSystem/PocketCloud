package de.pocketcloud.cloud.template;

import com.google.gson.annotations.JsonAdapter;
import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.cloud.template.util.conv.ServerSoftwareConverter;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.gson.TemplateJsonSerializer;
import de.pocketcloud.common.mapper.MapInline;
import de.pocketcloud.common.mapper.MapKey;
import de.pocketcloud.common.mapper.MapperUtils;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.component.BaseTemplate;
import de.pocketcloud.shared.sync.SyncType;

import java.nio.file.Path;
import java.util.Map;

@JsonAdapter(TemplateJsonSerializer.class)
public final class Template extends BaseTemplate implements SyncingElement<Template> {

    /**
     * Only meant for the SyncPacket
     */
    private transient boolean markedForRemoval = false;

    @MapInline
    private final TemplateSettings settings;
    @MapKey(converter = ServerSoftwareConverter.class)
    private final IServerSoftware serverSoftware;

    public Template(String name, TemplateSettings settings, TemplateType templateType, IServerSoftware serverSoftware) {
        super(name, settings, templateType, serverSoftware);
        this.settings = settings;
        this.serverSoftware = serverSoftware;
    }

    public Template markForRemoval() {
        this.markedForRemoval = true;
        return this;
    }

    @Override
    public void syncIn(Template data) {}

    @Override
    public void syncOut() {
        SyncPacket.create(SyncType.TEMPLATE, data -> data.write(this), Map.of("removal", markedForRemoval)).broadcast();
    }

    @Override
    public TemplateSettings settings() {
        return settings;
    }

    @Override
    public IServerSoftware serverSoftware() {
        return serverSoftware;
    }

    public Path path() {
        return PocketCloudPaths.templates().with(name).asPath();
    }

    public static Template read(Map<String, Object> map) {
        return MapperUtils.fromMap(map, Template.class);
    }
}