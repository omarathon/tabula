package uk.ac.warwick.courses.validators
import javax.validation.Validation
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import scala.reflect.BeanProperty


class CustomValidationTest extends TestBase {

	@Test def validationConstraints {
      val factory = Validation.buildDefaultValidatorFactory
      val validator = factory.getValidator

      validator.validate(new TestValidScalaObject("")) should not be('empty)
      validator.validate(new TestValidScalaObject("a")) should be('empty)
    }
	
}