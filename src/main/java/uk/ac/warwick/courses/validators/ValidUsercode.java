package uk.ac.warwick.courses.validators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy={ValidUsercodeValidator.class})
public @interface ValidUsercode {
	Class<? extends Payload>[] payload() default {};
	Class<?>[] groups() default {};
	String message();
}
