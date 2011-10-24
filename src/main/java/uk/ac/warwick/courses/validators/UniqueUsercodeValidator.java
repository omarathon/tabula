package uk.ac.warwick.courses.validators;

import java.util.Collection;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import scala.collection.Seq;

/**
 * Class-level annotation that checks that the value of a given field doesn't
 * currently exist in the specified collection.
 * 
 * It's not really anything specific to usercodes.
 * 
 * Supports java.util.Collection and scala.collection.Seq
 */
public class UniqueUsercodeValidator implements ConstraintValidator<UniqueUsercode, Object> {

	private String fieldName;
	private String collectionName;

	@Override
	public void initialize(UniqueUsercode annotation) {
		fieldName = annotation.fieldName();
		collectionName = annotation.collectionName();
	}

	@Override
	public boolean isValid(Object form, ConstraintValidatorContext ctx) {
		BeanWrapper bean = new BeanWrapperImpl(form);
		String usercode = (String) bean.getPropertyValue(fieldName);
		
		// Laboriously convert collection object to the correct thing
		Class<?> collectionType = bean.getPropertyType(collectionName);
		Object collectionObj = bean.getPropertyValue(collectionName);
		if (Collection.class.isAssignableFrom(collectionType)) {
			Collection<String> collection = (Collection<String>) collectionObj;
			return !collection.contains(usercode);
		} else if (Seq.class.isAssignableFrom(collectionType)) {
			Seq<String> collection = (Seq<String>) collectionObj;
			return !collection.contains(usercode);
		} else {
			throw new IllegalArgumentException("Unsupported collection type: " + collectionType.toString());
		}
	}

}
