package cz.neumimto.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by NeumimTo on 4.6.2015.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    String directoryPath() default "";
    String fileExt() default ".conf";
    String name() default "{id}";

    public static @interface Id {

    }
}
