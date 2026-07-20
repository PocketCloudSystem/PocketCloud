package de.pocketcloud.api.provider.write;

import de.pocketcloud.api.component.builder.ITemplateBuilder;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.ITemplateProvider;
import de.pocketcloud.api.template.util.TemplateEditData;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IWriteTemplateProvider extends ITemplateProvider {

    void create(ITemplateBuilder builder);

    void add(ITemplate template);

    void edit(ITemplate template, TemplateEditData newData);

    void remove(ITemplate template);

    void delete(ITemplate template);
}