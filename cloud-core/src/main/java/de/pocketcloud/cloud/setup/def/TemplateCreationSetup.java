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

import java.util.List;
import java.util.Map;

public final class TemplateCreationSetup extends Setup {

    @Override
    public void onStart(ILogger logger) {
        setPrefix("§bTemplate-Setup");
        logger.withoutFormat("Welcome to the Template-Setup!");
    }

    @Override
    public void onCancel() {
        CloudLogger.get().warn("The template setup was cancelled!");
    }

    @Override
    public List<Question<?>> applyQuestions() {
        return List.of(
                QuestionBuilder.builder("name", "What's the name of your template?")
                        .parser((input, error) -> {
                            if (PocketCloud.instance().templates().check(input)) {
                                error.set("A template with that name already exists!");
                                return null;
                            }
                            return input;
                        })
                        .build(),

                QuestionBuilder.builder("lobby", "Is your template a lobby?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("No", false)
                        .build(),

                QuestionBuilder.builder("maintenance", "Should your template be in maintenance?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("Yes", true)
                        .build(),

                QuestionBuilder.builder("static", "Should your template be static, meaning the servers of that template just have their own data?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("No", false)
                        .build(),

                QuestionBuilder.builder("alwaysCopyToStaticServers", "Should your static servers always copy data from their template?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("No", false)
                        .build(),

                QuestionBuilder.builder("saveOnShutdown", "Should your static servers always copy data from their template?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue("No", false)
                        .build(),

                QuestionBuilder.builder("maxPlayerCount", "How many players are allowed on that template's servers?")
                        .parser((input, _) -> input.matches("\\d+") ? Integer.parseInt(input) : null)
                        .defaultValue("20 players", 20)
                        .canSkipped(true)
                        .build(),

                QuestionBuilder.builder("minServerCount", "How many servers should always be running?")
                        .parser((input, _) -> input.matches("\\d+") ? Integer.parseInt(input) : null)
                        .defaultValue("1 server", 1)
                        .canSkipped(true)
                        .build(),

                QuestionBuilder.builder("maxServerCount", "How many servers can be running in total?")
                        .parser((input, _) -> input.matches("\\d+") ? Integer.parseInt(input) : null)
                        .defaultValue("2 servers", 2)
                        .canSkipped(true)
                        .build(),

                QuestionBuilder.builder("startNewPercentage", "How many players are required to start a new server? (in %, 0-100, 0 = none)")
                        .parser((input, _) -> {
                            if (!input.matches("\\d+(\\.\\d+)?")) return null;
                            double value = Double.parseDouble(input);
                            return (value < 0 || value > 100) ? null : value / 100;
                        })
                        .canSkipped(true)
                        .recommendation("75%")
                        .defaultValue("0% -> Disabled", 0.0)
                        .build(),

                QuestionBuilder.builder("autoStart", "Should your template start servers automatically?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .recommendation("yes")
                        .defaultValue("Yes", true)
                        .build(),

                QuestionBuilder.builder("maxMemory", "How much memory do servers from this template have? §8(§bin Megabytes§8)")
                        .parser((input, _) -> {
                            if (!input.matches("\\d+")) return null;
                            int value = Integer.parseInt(input);
                            return (value < 0) ? null : value;
                        })
                        .canSkipped(true)
                        .recommendation("1024 Megabytes")
                        .defaultValue("1024", 1024)
                        .build(),

                QuestionBuilder.builder("serverSoftware", "What server software should your template use?")
                        .parser((input, error) -> {
                            try {
                                IServerSoftware software = PocketCloud.instance().softwares().get(input).orElseThrow(IllegalStateException::new);
                                TemplateType.valueOf(software.templateType());
                                return software;
                            } catch (IllegalArgumentException e) {
                                error.set("No software found with that name!");
                                return null;
                            }
                        })
                        .canSkipped(false)
                        .possibleAnswers(PocketCloud.instance().softwares().getAll().stream().map(IServerSoftware::name).toList())
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
                .autoStart((Boolean) results.get("autoStart"))
                .saveOnShutdown((Boolean) results.get("saveOnShutdown"))
                .startNewPercentage((Double) results.get("startNewPercentage"))
                .maxPlayerCount((Integer) results.get("maxPlayerCount"))
                .minServerCount((Integer) results.get("minServerCount"))
                .maxServerCount((Integer) results.get("maxServerCount"))
                .maxMemory((Integer) results.get("maxMemory"))
                .software(software)
                .type(software.type())
        );
    }
}