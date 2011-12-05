package uk.ac.warwick.courses.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;

public class SpelAssertValidator implements ConstraintValidator<SpelAssert, Object> {

	@Autowired private ExpressionParser parser;
	private Expression expression;
	private String message;
	private String targetProperty;
	
	@Override
	public void initialize(SpelAssert annotation) {
		message = annotation.message();
		targetProperty = annotation.targetProperty();
		expression = parser.parseExpression(annotation.value());
	}

	@Override
	public boolean isValid(Object object, ConstraintValidatorContext ctx) {
		if (object == null) return true;
		else {
			boolean valid = expression.getValue(object, Boolean.class);
			if (!valid) {
				if (!"".equals(targetProperty)) {
					ctx.disableDefaultConstraintViolation();
					ConstraintViolationBuilder builder = ctx.buildConstraintViolationWithTemplate(message);
					builder.addNode(targetProperty).addConstraintViolation();
				}
			}
			return valid;
		}
	}

}
