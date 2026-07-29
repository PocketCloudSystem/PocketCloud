package de.pocketcloud.cloud.setup.def;

import de.pocketcloud.api.language.ILanguage;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.config.MainConfig;
import de.pocketcloud.cloud.config.sub.HttpServerConfiguration;
import de.pocketcloud.cloud.config.sub.NetworkConfiguration;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.setup.Question;
import de.pocketcloud.cloud.setup.QuestionBuilder;
import de.pocketcloud.cloud.setup.Setup;

import java.util.List;
import java.util.Map;

public final class ConfigSetup extends Setup {

    @Override
    public void onStart(ILogger logger) {
        setPrefix("§bConfiguration-Setup");
        logger.withoutFormat("Welcome to the Configuration Setup!");
    }

    @Override
    public void onCancel() {
        CloudLogger.get().warn("The configuration setup was cancelled!");
    }

    @Override
    public List<Question<?>> applyQuestions() {
        MainConfig config = PocketCloud.instance().config();

        return List.of(
                QuestionBuilder.builder("cloudName", "What's the name of the cloud?")
                        .defaultValue(config.cloudName(), config.cloudName())
                        .canSkipped(true)
                        .build(),

                QuestionBuilder.builder("language", "What language should the cloud use?")
                        .canSkipped(true)
                        .possibleAnswers(PocketCloud.instance().language().getAll().stream().map(ILanguage::id).toArray(String[]::new))
                        .defaultValue(PocketCloud.instance().language().current().id(), PocketCloud.instance().language().current().id())
                        .build(),

                QuestionBuilder.builder("writeTimingsOnShutdown", "Should timings be written on shutdown?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(config.writeTimingsOnShutdown() ? "Yes" : "No", config.writeTimingsOnShutdown())
                        .build(),

                QuestionBuilder.builder("networkAddress", "What's the network address?")
                        .canSkipped(true)
                        .defaultValue(config.network().address(), config.network().address())
                        .build(),

                QuestionBuilder.builder("networkPort", "What's the network port?")
                        .parser((input, _) -> {
                            if (!input.matches("\\d+")) return null;
                            int port = Integer.parseInt(input);
                            return (port <= 0 || port > 65535) ? null : port;
                        })
                        .canSkipped(true)
                        .defaultValue(String.valueOf(config.network().port()), config.network().port())
                        .build(),

                QuestionBuilder.builder("networkEncryption", "Should network encryption be enabled?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(config.network().encryption() ? "Yes" : "No", config.network().encryption())
                        .build(),

                QuestionBuilder.builder("httpServerEnabled", "Should the http server be enabled?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(config.httpServer().enabled() ? "Yes" : "No", config.httpServer().enabled())
                        .build(),

                QuestionBuilder.builder("httpServerAddress", "What's the http server address?")
                        .canSkipped(true)
                        .defaultValue(config.httpServer().address(), config.httpServer().address())
                        .build(),

                QuestionBuilder.builder("httpServerPort", "What's the http server port?")
                        .parser((input, _) -> {
                            if (!input.matches("\\d+")) return null;
                            int port = Integer.parseInt(input);
                            return (port <= 0 || port > 65535) ? null : port;
                        })
                        .canSkipped(true)
                        .defaultValue(String.valueOf(config.httpServer().port()), config.httpServer().port())
                        .build()
        );
    }

    @Override
    public void handleResults(Map<String, Object> results) {
        MainConfig config = PocketCloud.instance().config();

        config.cloudName((String) results.get("cloudName"));
        config.language((String) results.get("language"));
        config.writeTimingsOnShutdown((Boolean) results.get("writeTimingsOnShutdown"));

        NetworkConfiguration networkConfiguration = new NetworkConfiguration();
        networkConfiguration.address((String) results.get("networkAddress"));
        networkConfiguration.port((Integer) results.get("networkPort"));
        networkConfiguration.encryption((Boolean) results.get("networkEncryption"));

        HttpServerConfiguration httpServerConfiguration = new HttpServerConfiguration();
        httpServerConfiguration.enabled((Boolean) results.get("httpServerEnabled"));
        httpServerConfiguration.address((String) results.get("httpServerAddress"));
        httpServerConfiguration.port((Integer) results.get("httpServerPort"));

        config.network(networkConfiguration);
        config.httpServer(httpServerConfiguration);

        config.save();
        CloudLogger.get().success("Your configuration has been §asaved§r. Restart the cloud to apply the changes made.");
    }
}