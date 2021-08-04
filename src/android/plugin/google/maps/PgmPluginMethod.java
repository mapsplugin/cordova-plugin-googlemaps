package plugin.google.maps;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)

@Target({
    ElementType.METHOD
})

@SuppressWarnings("unused")
public @interface PgmPluginMethod {
    boolean runOnUiThread() default false;
}
