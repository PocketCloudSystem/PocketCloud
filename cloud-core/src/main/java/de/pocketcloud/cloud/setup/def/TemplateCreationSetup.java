package de.pocketcloud.cloud.setup.def;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.builder.TemplateBuilder;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.setup.Question;
import de.pocketcloud.cloud.setup.QuestionBuilder;
import de.pocketcloud.cloud.setup.Setup;
import de.pocketcloud.cloud.setup.SetupHelper;

import java.util.List;
import java.util.Map;

public final class TemplateCreationSetup extends Setup {

    @Override
    public void onStart(ILogger logger) {
        setPrefix("§bTemplate-Setup");
        logger.withoutFormat("Welcome to the Template-Setup!");
        logger.withoutFormat("You can skip most questions with §eEnter §rto use the default value.");
    }

    @Override
    public void onCancel() {
        CloudLogger.get().warn("The template setup was cancelled!");
    }

    @Override
    public List<Question<?>> applyQuestions() {
        return List.of(
                QuestionBuilder.builder("name", "What's the name of your template?")
                        .parser((input, error) -> SetupHelper.parseTemplateName(input, error, true))
                        .canSkipped(false)
                        .build(),

                QuestionBuilder.builder("lobby", "Is this template a lobby?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("No", false)
                        .build(),

                QuestionBuilder.builder("maintenance", "Should this template start in maintenance mode?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("Yes", true)
                        .build(),

                QuestionBuilder.builder("static", "Should servers of this template be static (keep their own persistent data)?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("No", false)
                        .build(),

                QuestionBuilder.builder("alwaysCopyToStaticServers", "Should static servers always copy data from the template on start? (only relevant if static = yes)")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("No", false)
                        .build(),

                QuestionBuilder.builder("saveOnShutdown", "Should servers save their data back to the template on shutdown?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("No", false)
                        .build(),

                QuestionBuilder.builder("deleteOnStop", "Should the server directory be deleted after shutdown?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("Yes", true)
                        .build(),

                QuestionBuilder.builder("stopOnEmpty", "Should servers automatically shut down when they reach 0 players?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("No", false)
                        .build(),

                QuestionBuilder.builder("autoStart", "Should the cloud automatically start servers of this template?")
                        .parser((input, _) -> SetupHelper.parseBoolean(input))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .recommendation("yes")
                        .defaultValue("Yes", true)
                        .build(),

                QuestionBuilder.builder("startNewServerThreshold", "Player load threshold to start a new server (0-100 %, 0 = disabled)")
                        .parser((input, error) -> SetupHelper.parseDouble(input, error, 0, 100, true))
                        .canSkipped(true)
                        .recommendation("75")
                        .defaultValue("0 (disabled)", 0.0)
                        .build(),

                QuestionBuilder.builder("maxPlayerCount", "Maximum players per server of this template")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, 100_000))
                        .canSkipped(true)
                        .defaultValue("20", 20)
                        .build(),

                QuestionBuilder.builder("minServerCount", "How many servers should always be online (minimum)?")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, 100))
                        .canSkipped(true)
                        .defaultValue("1", 1)
                        .build(),

                QuestionBuilder.builder("maxServerCount", "Maximum number of servers that can run for this template")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, 100))
                        .canSkipped(true)
                        .defaultValue("2", 2)
                        .build(),

                QuestionBuilder.builder("maxMemory", "Memory (RAM) per server in Megabytes")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 256, (int) Math.max(PocketCloud.instance().performanceStats().systemTotalMemory() / 1024 / 1024, 1024)))
                        .canSkipped(true)
                        .recommendation("1024")
                        .defaultValue("1024", 1024)
                        .build(),

                QuestionBuilder.builder("startupTimeout", "Seconds to wait for a server to start successfully")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 5, 120))
                        .canSkipped(true)
                        .recommendation("15")
                        .defaultValue("15", 15)
                        .build(),

                QuestionBuilder.builder("shutdownTimeout", "Seconds to wait for a server to stop cleanly")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 5, 120))
                        .canSkipped(true)
                        .recommendation("15")
                        .defaultValue("15", 15)
                        .build(),

                QuestionBuilder.builder("emptyServerGracePeriod", "Seconds to wait after 0 players before stopping the server (only if stopOnEmpty = yes)")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, Integer.MAX_VALUE))
                        .canSkipped(true)
                        .recommendation("120")
                        .defaultValue("120", 120)
                        .build(),

                QuestionBuilder.builder("priority", "Boot priority of this template (higher = more important, used for start order)")
                        .parser((input, error) -> SetupHelper.parseInt(input, error, 0, Integer.MAX_VALUE))
                        .canSkipped(true)
                        .recommendation("0")
                        .defaultValue("0", 0)
                        .build(),

                QuestionBuilder.builder("serverSoftware", "Which server software should this template use?")
                        .parser((input, error) -> {
                            var softwareOpt = PocketCloud.instance().softwares().get(input);
                            if (softwareOpt.isEmpty()) {
                                error.set("No software found with that name!");
                                return null;
                            }

                            IServerSoftware software = softwareOpt.get();
                            try {
                                TemplateType.valueOf(software.templateType());
                            } catch (IllegalArgumentException e) {
                                error.set("Software has an invalid template type!");
                                return null;
                            }

                            return software;
                        })
                        .canSkipped(false)
                        .possibleAnswers(PocketCloud.instance().softwares().getAll().stream()
                                .map(IServerSoftware::name)
                                .toList())
                        .build()
        );
    }

    @Override
    public void handleResults(Map<String, Object> results) {
        IServerSoftware software = (IServerSoftware) results.get("serverSoftware");

        PocketCloud.instance().templates().create(TemplateBuilder.create()
                .name((String) results.get("name"))
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
                .software(software)
                .type(software.type())
        );
    }
}