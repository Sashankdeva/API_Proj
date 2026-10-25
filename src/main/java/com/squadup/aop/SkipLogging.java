package com.squadup.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Keeps a service method (or every method of a class) out of the debug trace written by
 * {@link LoggingAspect}. Use it where arguments or results are secrets, such as bearer tokens.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkipLogging {
}
