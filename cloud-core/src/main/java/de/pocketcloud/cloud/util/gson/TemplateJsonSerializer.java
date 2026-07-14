package de.pocketcloud.cloud.util.gson;

import com.google.gson.*;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.settings.TemplateSettings;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.template.Template;

import java.lang.reflect.Type;
import java.util.Map;

public final class TemplateJsonSerializer implements JsonSerializer<Template>, JsonDeserializer<Template> {

    @Override
    public JsonElement serialize(Template template, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        object.add("name", new JsonPrimitive(template.name()));
        for (Map.Entry<String, JsonElement> pair : jsonSerializationContext.serialize(template.settings()).getAsJsonObject().entrySet()) {
            object.add(pair.getKey(), pair.getValue());
        }

        object.add("templateType", jsonSerializationContext.serialize(template.templateType()));
        object.add("serverSoftware", new JsonPrimitive(template.serverSoftware().name()));

        return object;
    }

    @Override
    public Template deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject object = jsonElement.getAsJsonObject();

        String name = object.get("name").getAsString();
        TemplateSettings settings = jsonDeserializationContext.deserialize(object, TemplateSettings.class);
        TemplateType templateType = TemplateType.valueOf(object.get("templateType").getAsString());
        ServerSoftware software = PocketCloud.instance().software().get(object.get("serverSoftware").getAsString());

        return new Template(name, settings, templateType, software);
    }
}