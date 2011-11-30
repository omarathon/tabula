package uk.ac.warwick.courses.validators;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Define a validation rule as a SpEL expression which can access
 * all the properties of the class.
 * <p>
 * For multiple SpEL assertions, you can wrap them in SpelAssert.List
 * <p>
 * http://static.springsource.org/spring/docs/current/spring-framework-reference/html/expressions.html
 */
@Target( ElementType.TYPE )
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=SpelAssertValidator.class)
@Documented
public @interface SpelAssert {

    String message() default "{expression.failed}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String value();

}
