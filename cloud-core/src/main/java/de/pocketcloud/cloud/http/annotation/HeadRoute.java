package de.pocketcloud.cloud.http.annotation;

import de.pocketcloud.cloud.http.auth.DefaultAuthFailedHandler;
import de.pocketcloud.cloud.http.auth.DefaultAuthentication;
import de.pocketcloud.cloud.http.auth.IAuthentication;
import de.pocketcloud.cloud.http.handler.AuthenticationFailedHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HeadRoute {

    String value();

    Class<? extends IAuthentication> authentication() default DefaultAuthentication.class;

    Class<? extends AuthenticationFailedHandler> onAuthFailed() default DefaultAuthFailedHandler.class;
}