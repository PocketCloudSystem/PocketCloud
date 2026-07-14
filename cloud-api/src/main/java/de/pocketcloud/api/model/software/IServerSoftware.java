package de.pocketcloud.api.model.software;

import de.pocketcloud.api.template.TemplateType;

public interface IServerSoftware {

    String name();

    String templateType();

    ISoftwareDownload download();

    ISoftwareBinary binary();

    ISoftwareBridge bridge();

    ISoftwareConfig config();

    TemplateType type();
}