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
	 * 
	 * There was a problem with it working on a Scala object at first
	 * but it was because the annotation was on a constructor arg, and
	 * it was being applied to that instead of the property. There are
	 * some Scala meta-annotations to tell the compiler where to attach
	 * the annotation, or you can just not use the constructor like that.
	 */
	@Test def customValidationConstraints {
      val factory = Validation.buildDefaultValidatorFactory
      val validator = factory.getValidator
				
	  val scalaObj = new TestValidScalaObject("ron")
	  val javaObj = new TestValidJavaObject("ron")
	  
      validator.validate(javaObj) should not be('empty)
      validator.validate(scalaObj) should not be('empty)
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