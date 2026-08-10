package de.pocketcloud.cloud.console.command.desc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandDescription {

    String name();

    String description();

    String usage() default "";

    String[] aliases() default {};
}
