package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import org.springframework.web.bind.WebDataBinder
import org.springframework.validation.Validator
import org.mockito.Matchers._
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.validators.CompositeValidator
import uk.ac.warwick.tabula.validators.ClassValidator

class BaseControllerTest extends TestBase with Mockito {
	
	@Test def bindingDefault {
		val controller = new BaseController {}
		
		val binder = new WebDataBinder(new Object)
		controller._binding(binder)
		
		binder.getValidator() should be (null)
		binder.getDisallowedFields() should be (Array())
	}
	
	@Test def bindingWithExistingValidator {
		val controller = new BaseController {}
		
		val binder = new WebDataBinder(new Object)
		
		val validator = mock[Validator]
		validator.supports(isA(classOf[Class[_]])) returns (true)
		
		binder.setValidator(validator)
		
		controller._binding(binder)
		
		binder.getValidator() should be (validator)
		binder.getDisallowedFields() should be (Array())
	}
	
	@Test def selfValidates {
		val command = new SelfValidating {
			def validate(errors: Errors) {}
		}
		
		val controller = new BaseController {
			validatesSelf[SelfValidating]
		}
		
		controller.validator match {
			case _: ClassValidator[SelfValidating] =>
			case _ => fail()
		}
		
		val binder = new WebDataBinder(new Object)
		
		val validator = mock[Validator]
		validator.supports(isA(classOf[Class[_]])) returns (true)
		
		binder.setValidator(validator)
		controller._binding(binder)
		
		binder.getValidator() match {
			case v: CompositeValidator => {
				v.list.length should be (2)
				v.list(0).isInstanceOf[ClassValidator[SelfValidating]] should be (true)
				v.list(1) should be (validator)
			}
			case _ => fail()
		}
		binder.getDisallowedFields() should be (Array())
	}
	
	@Test def nukeOriginal {
		val command = new SelfValidating {
			def validate(errors: Errors) {}
		}
		
		val controller = new BaseController {
			onlyValidatesWith{ (obj: SelfValidating, errors) => }
		}
		
		controller.validator match {
			case _: ClassValidator[SelfValidating] =>
			case _ => fail()
		}
		
		val binder = new WebDataBinder(new Object)
		
		val validator = mock[Validator]
		validator.supports(isA(classOf[Class[_]])) returns (true)
		
		binder.setValidator(validator)
		controller._binding(binder)
		
		// original validator was nuked by "onlyValidatesWith"
		binder.getValidator() should not be (validator)
		binder.getValidator().isInstanceOf[ClassValidator[SelfValidating]] should be (true)
		binder.getDisallowedFields() should be (Array())
	}
	
	@Test def disallowedFields {
		val controller = new BaseController {}
		controller.disallowedFields = List("steve", "yes")
		
		val binder = new WebDataBinder(new Object)
		controller._binding(binder)
		
		binder.getDisallowedFields() should be (Array("steve", "yes"))
	}

}