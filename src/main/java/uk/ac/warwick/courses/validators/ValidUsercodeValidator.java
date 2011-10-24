package uk.ac.warwick.courses.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.util.core.StringUtils;

public class ValidUsercodeValidator implements ConstraintValidator<ValidUsercode, String> {

	@Autowired private UserLookup userLookup;
	
	public void initialize(ValidUsercode annotation) {}

	public boolean isValid(String code, ConstraintValidatorContext ctx) {
		if (!StringUtils.hasText(code)) {
			return true;
		}
		boolean found = userLookup.getUserByUserId(code).isFoundUser();
		return found;
	}

}
