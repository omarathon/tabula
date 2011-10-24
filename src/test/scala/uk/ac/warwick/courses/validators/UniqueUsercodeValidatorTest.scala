package uk.ac.warwick.courses.validators

import scala.collection.immutable.List
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.TestBase
import org.junit.Test

class UniqueUsercodeValidatorTest extends TestBase {

  @Test def valid {
    val form = new Form
    form.usercode = "jensen"
    form.currentList = List("ronson", "benson")
    
    val validator = new UniqueUsercodeValidator
    
  }

}