package de.pocketcloud.cloud.setup.def;

import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.api.template.util.TemplateEditData;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.setup.Question;
import de.pocketcloud.cloud.setup.QuestionBuilder;
import de.pocketcloud.cloud.setup.Setup;
import de.pocketcloud.cloud.setup.SetupHelper;
import de.pocketcloud.cloud.template.Template;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public final class TemplateEditSetup extends Setup {

    private final Template template;

    @Override
    public void onStart(ILogger logger) {
        setPrefix("§bTemplate-Editing-Setup");
        logger.withoutFormat("Welcome to the Template-Editing-Setup!");
        logger.withoutFormat("Editing template: §b" + template.name());
        logger.withoutFormat("You can skip questions with Enter to keep the current value.");
    }

    @Override
    public void onCancel() {
        CloudLogger.get().warn("The template editing was cancelled!");
    }

    @Override
    public List<Question<?>> applyQuestions() {
        var settings = template.settings();

        return List.of(
                QuestionBuilder.builder("lobby", "Is this template a lobby?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(settings.lobby() ? "Yes" : "No", settings.lobby())
                        .build(),

                QuestionBuilder.builder("maintenance", "Should this template be in maintenance mode?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(settings.maintenance() ? "Yes" : "No", settings.maintenance())
                        .build(),

                QuestionBuilder.builder("static", "Should servers of this template be static (keep their own persistent data)?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(settings.staticServers() ? "Yes" : "No", settings.staticServers())
                        .build(),

                QuestionBuilder.builder("alwaysCopyToStaticServers", "Should static servers always copy data from the template on start? (only relevant if static = yes)")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(settings.alwaysCopyToStaticServers() ? "Yes" : "No", settings.alwaysCopyToStaticServers())
                        .build(),

                QuestionBuilder.builder("saveOnShutdown", "Should servers save their data back to the template on shutdown?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(settings.saveOnShutdown() ? "Yes" : "No", settings.saveOnShutdown())
                        .build(),

                QuestionBuilder.builder("deleteOnStop", "Should the server directory be deleted after shutdown?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(settings.deleteOnStop() ? "Yes" : "No", settings.deleteOnStop())
                        .build(),

                QuestionBuilder.builder("stopOnEmpty", "Should servers automatically shut down when they reach 0 players?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(settings.stopOnEmpty() ? "Yes" : "No", settings.stopOnEmpty())
                        .build(),

                QuestionBuilder.builder("autoStart", "Should the cloud automatically start servers of this template?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .recommendation("yes")
                        .defaultValue(settings.autoStart() ? "Yes" : "No", settings.autoStart())
                        .build(),

                QuestionBuilder.builder("startNewServerThreshold", "Player load threshold to start a new server (0-100 %, 0 = disabled)")
                        .parser((input, error) -> SetupHelper.parseDouble(input, error, 0, 100, true))
                        .canSkipped(true)
                        .recommendation("75")
                        .defaultValue(settings.startNewServerThreshold() == 0.0 ? "0 (disabled)" : String.valueOf(settings.startNewServerThreshold()), settings.startNewServerThreshold())
                        .build(),

                QuestionBuilder.builder("maxPlayerCount", "Maximum players per server of this template")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, 100_000))
                        .canSkipped(true)
                        .defaultValue(String.valueOf(settings.maxPlayerCount()), settings.maxPlayerCount())
                        .build(),

                QuestionBuilder.builder("minServerCount", "How many servers should always be online (minimum)?")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, 100))
                        .canSkipped(true)
                        .defaultValue(String.valueOf(settings.minServerCount()), settings.minServerCount())
                        .build(),

                QuestionBuilder.builder("maxServerCount", "Maximum number of servers that can run for this template")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, 100))
                        .canSkipped(true)
                        .defaultValue(String.valueOf(settings.maxServerCount()), settings.maxServerCount())
                        .build(),

                QuestionBuilder.builder("maxMemory", "Memory (RAM) per server in Megabytes")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 256, (int) Math.max(PocketCloud.instance().performanceStats().systemTotalMemory() / 1024 / 1024, 1024)))
                        .canSkipped(true)
                        .recommendation("1024")
                        .defaultValue(String.valueOf(settings.maxMemory()), settings.maxMemory())
                        .build(),

                QuestionBuilder.builder("startupTimeout", "Seconds to wait for a server to start successfully")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 5, 120))
                        .canSkipped(true)
                        .recommendation("15")
                        .defaultValue(String.valueOf(settings.startupTimeout()), settings.startupTimeout())
                        .build(),

                QuestionBuilder.builder("shutdownTimeout", "Seconds to wait for a server to stop cleanly")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 5, 120))
                        .canSkipped(true)
                        .recommendation("15")
                        .defaultValue(String.valueOf(settings.shutdownTimeout()), settings.shutdownTimeout())
                        .build(),

                QuestionBuilder.builder("emptyServerGracePeriod", "Seconds to wait after 0 players before stopping the server (only if stopOnEmpty = yes)")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, Integer.MAX_VALUE))
                        .canSkipped(true)
                        .recommendation("120")
                        .defaultValue(String.valueOf(settings.emptyServerGracePeriod()), settings.emptyServerGracePeriod())
                        .build(),

                QuestionBuilder.builder("priority", "Boot priority of this template (higher = more important, used for start order)")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, Integer.MAX_VALUE))
                        .canSkipped(true)
                        .recommendation("0")
                        .defaultValue(String.valueOf(settings.priority()), settings.priority())
                        .build()
        );
    }

    @Override
    public void handleResults(Map<String, Object> results) {
        PocketCloud.instance().templates().edit(template, TemplateEditData.create()
                .lobby((Boolean) results.get("lobby"))
                .maintenance((Boolean) results.get("maintenance"))
                .staticServers((Boolean) results.get("static"))
                .alwaysCopyToStaticServers((Boolean) results.get("alwaysCopyToStaticServers"))
                .saveOnShutdown((Boolean) results.get("saveOnShutdown"))
                .deleteOnStop((Boolean) results.get("deleteOnStop"))
                .stopOnEmpty((Boolean) results.get("stopOnEmpty"))
                .autoStart((Boolean) results.get("autoStart"))
                .startNewServerThreshold((Double) results.get("startNewServerThreshold"))
                .maxPlayerCount((Integer) results.get("maxPlayerCount"))
                .minServerCount((Integer) results.get("minServerCount"))
                .maxServerCount((Integer) results.get("maxServerCount"))
                .maxMemory((Integer) results.get("maxMemory"))
                .startupTimeout((Integer) results.get("startupTimeout"))
                .shutdownTimeout((Integer) results.get("shutdownTimeout"))
                .emptyServerGracePeriod((Integer) results.get("emptyServerGracePeriod"))
                .priority((Integer) results.get("priority"))
        );
    }
}