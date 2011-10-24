package uk.ac.warwick.courses.validators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy={HasLetterValidator.class})
public @interface HasLetter {
	char letter();
	Class<? extends Payload>[] payload() default {};
	Class<?>[] groups() default {};
	String message() default "Didn't contain expected letter";
}
