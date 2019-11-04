package uk.ac.warwick.tabula.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = WithinYearsValidatorForLocalDate.class)
public @interface DateWithinYears {

  int maxFuture() default Integer.MAX_VALUE;

  int maxPast() default Integer.MAX_VALUE;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String message() default "{uk.ac.warwick.tabula.validators.WithinYears.message}";

}
