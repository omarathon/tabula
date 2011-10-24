package uk.ac.warwick.courses.validators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy={UniqueUsercodeValidator.class})
public @interface UniqueUsercode {
	Class<? extends Payload>[] payload() default {};
	Class<?>[] groups() default {};
	String message();
	
	String fieldName();
	String collectionName();
}
