package uk.ac.warwick.tabula.validators

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.MockUserLookup
import org.springframework.validation.BindException
import scala.jdk.CollectionConverters._

class UsercodeListValidatorTest extends TestBase {

  class ValidateMe {
    var usercodes: List[String] = _
  }

  @Test def passes(): Unit = {
    val errors = new BindException(new ValidateMe(), "command")

    val userLookup = new MockUserLookup
    userLookup.registerUsers("cuscav", "cusebr")

    val validator = new UsercodeListValidator(List("cuscav", "cusebr").asJava, "usercodes")
    validator.userLookup = userLookup

    validator.validate(errors)

    errors.hasErrors should be(false)
  }

  @Test def notEmpty(): Unit = {
    val errors = new BindException(new ValidateMe(), "command")

    val userLookup = new MockUserLookup
    userLookup.registerUsers("cuscav", "cusebr")

    val validator = new UsercodeListValidator(List("").asJava, "usercodes")
    validator.userLookup = userLookup

    validator.validate(errors)

    errors.hasErrors should be(true)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("usercodes")
    errors.getFieldError.getCode should be("NotEmpty")
  }

  @Test def alreadyHasCode(): Unit = {
    val errors = new BindException(new ValidateMe(), "command")

    val userLookup = new MockUserLookup
    userLookup.registerUsers("cuscav", "cusebr")

    val validator = new UsercodeListValidator(List("cuscav", "cusebr").asJava, "usercodes") {
      override def alreadyHasCode = true
    }

    validator.userLookup = userLookup

    validator.validate(errors)

    errors.hasErrors should be(true)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("usercodes")
    errors.getFieldError.getCode should be("userId.duplicate")
  }

  @Test def invalidUser(): Unit = {
    val errors = new BindException(new ValidateMe(), "command")

    val userLookup = new MockUserLookup
    userLookup.registerUsers("cuscav") // cusebr is not found

    val validator = new UsercodeListValidator(List("cuscav", "cusebr").asJava, "usercodes")
    validator.userLookup = userLookup

    validator.validate(errors)

    errors.hasErrors should be(true)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("usercodes")
    errors.getFieldError.getCode should be("userId.notfound.specified")
  }

}
