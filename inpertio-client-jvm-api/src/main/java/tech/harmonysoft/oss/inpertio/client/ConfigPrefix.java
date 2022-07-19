package tech.harmonysoft.oss.inpertio.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     It's quite often when we group related properties under some config hierarchy, for example:
 * </p>
 * <pre>
 *     my-app:
 *       db:
 *         address: some-address
 *         login: some-login
 *         password: some-password
 * </pre>
 * <p>
 *     In this situation it's natural to have a config class like below:
 * </p>
 * <pre>
 *    {@literal @}ConfigPrefix("my-app.db")
 *     class MyDbConfig {
 *         String address;
 *         String login;
 *         String password;
 *     }
 * </pre>
 * <p>
 *     Here we define common property prefix in the class annotation.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigPrefix {

    String value() default "";
}