package kr.chuyong.springspigot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandMapping {
    String value() default "";

    String child() default "";

    String usage() default "";

    String prefix() default "";

    String perm() default "";

    String error() default "명령어 사용법이 올바르지 않습니다";

    String noPerm() default "이 명령어를 실행할 권한이 없습니다";

    String noConsole() default "콘솔에서 사용할 수 없습니다";

    String[] aliases() default {};

    String[] subcommand() default {};

    int minArgs() default 0;

    int maxArgs() default 100;

    boolean op() default false;

    boolean console() default false;
}
