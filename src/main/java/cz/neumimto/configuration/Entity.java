package cz.neumimto.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by NeumimTo on 4.6.2015.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    String directoryPath() default "";
    String fileExt() default ".conf";
    String name() default "{id}";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Id {

    }
}
