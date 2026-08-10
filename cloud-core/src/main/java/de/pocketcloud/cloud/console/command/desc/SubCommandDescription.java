package de.pocketcloud.cloud.console.command.desc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommandDescription {

    String name();

    String usage() default "";

    boolean optional() default false;
}
