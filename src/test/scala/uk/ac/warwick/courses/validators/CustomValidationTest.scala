package uk.ac.warwick.courses.validators
import javax.validation.Validation
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import scala.reflect.BeanProperty

@UniqueUsercode(fieldName="usercode", collectionName="currentList", message="{value} already exists")
class Form {
  @BeanProperty var usercode:String = _
  @BeanProperty var currentList = List[String]()
}

class CustomValidationTest extends TestBase {
  
	/**
	 * Check that custom annotation stuff is applied.
	 */
	@Test def customValidationConstraints {
      val factory = Validation.buildDefaultValidatorFactory
      val validator = factory.getValidator
				
	  val scalaObj = new TestValidScalaObject("ron")
	  val javaObj = new TestValidJavaObject("ron")
	  
      val javaViolations = validator.validate(javaObj)
      javaViolations.size should be(1)
      
      val scalaViolations = validator.validate(scalaObj)
      scalaViolations.size should be(1)
    }
	
	@Test def userIsUnique {
	  val factory = Validation.buildDefaultValidatorFactory
      val validator = factory.getValidator
      
      val form = new Form
      form.usercode = "jensen"
      form.currentList = List("ronson", "benson")
      val violations = validator.validate(form)
      violations.size should be (0)
	}
	
	@Test def userIsNonUnique {
	  val factory = Validation.buildDefaultValidatorFactory
      val validator = factory.getValidator
      
      val form = new Form
      form.usercode = "jensen"
      form.currentList = List("ronson", "benson", "jensen")
      val violations = validator.validate(form)
      violations.size should be (1)
	}
}