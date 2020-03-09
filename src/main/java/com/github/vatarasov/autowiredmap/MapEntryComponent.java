package com.github.vatarasov.autowiredmap;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(MapEntryComponents.class)
@Component
public @interface MapEntryComponent {
    String map();
    String key();
}
