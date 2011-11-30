package uk.ac.warwick.courses.validators
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.joda.time.Weeks
import org.junit.runner.RunWith
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.test.context.ContextConfiguration
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import javax.validation.Validation
import uk.ac.warwick.courses.TestBase
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.support.AnnotationConfigContextLoader
import javax.validation.Validator
import javax.validation.ValidatorFactory

/*
 * Fire up a mini Spring context because Spring's LocalValidatorFactoryBean sets
 * up constraint validators to be autowired and such, which we rely on.
 */
@Configuration
class SpelAssertValidatorConfig {
    @Bean def validator = new LocalValidatorFactoryBean
    @Bean def parser = new SpelExpressionParser
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(loader=classOf[AnnotationConfigContextLoader], classes=Array(classOf[SpelAssertValidatorConfig]))
class SpelAssertValidatorTest extends TestBase {

  @Autowired var factory:ValidatorFactory = _
  val TWO_WEEKS = Weeks.TWO
  
  @Test def expressionOnForm {
    val validator = factory.getValidator
    val form = new MyForm
    
    validator.validate(form) should not be ('empty)
    
    form.closeDate = form.closeDate plus TWO_WEEKS
    
    validator.validate(form) should be ('empty)
  }

  // SpEL seems to understand how to compare Joda Time objects, which is nice 
  @SpelAsserts(Array(
      new SpelAssert(value="5 < 7"), // will never fail - just checking multiple asserts works
      new SpelAssert(value="openDate < closeDate")
  ))
  class MyForm {
    @BeanProperty var openDate:DateTime = new DateTime
    @BeanProperty var closeDate:DateTime = openDate
  }
  
}