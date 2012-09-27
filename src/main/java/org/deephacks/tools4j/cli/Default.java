package org.deephacks.tools4j.cli;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Responsible for providing default values to method arguments. That is, if 
 * the user did not provide an argument, the default value will be used.
 */
@Target({ PARAMETER })
@Retention(RUNTIME)
public @interface Default {
    public String value();
}
