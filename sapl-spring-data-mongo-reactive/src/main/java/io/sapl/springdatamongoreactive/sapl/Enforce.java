package io.sapl.springdatamongoreactive.sapl;

import org.springframework.stereotype.Component;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Enforce {

    String subject() default "";

    String action() default "";

    String resource() default "";

    String environment() default "";

    Class<?>[] staticClasses() default Class.class;

}