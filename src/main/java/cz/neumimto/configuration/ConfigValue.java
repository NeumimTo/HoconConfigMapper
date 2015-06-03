package cz.neumimto.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigValue {
    String name() default "";

    Class<? extends IMarshaller> as() default MarshallerImpl.class;
}
