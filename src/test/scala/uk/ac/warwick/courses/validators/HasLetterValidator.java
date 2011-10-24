package uk.ac.warwick.courses.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class HasLetterValidator implements ConstraintValidator<HasLetter, String> {

	private char letter;

	public void initialize(HasLetter annotation) {
		letter = annotation.letter();
	}

	public boolean isValid(String text, ConstraintValidatorContext ctx) {
		return text.indexOf(letter) > -1;
	}

}