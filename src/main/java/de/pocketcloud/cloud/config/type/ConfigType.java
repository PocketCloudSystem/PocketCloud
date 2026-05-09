package de.pocketcloud.cloud.config.type;

import java.util.Map;

public interface ConfigType {

    Map<String, Object> decode(String content);

    String encode(Map<String, Object> content);
}