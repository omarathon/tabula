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
import org.hibernate.SessionFactory
import org.hibernate.classic.Session
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.SelfValidating

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
		validator.supports(isA[Class[_]]) returns (true)
		
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
			case _: ClassValidator[_] =>
			case _ => fail()
		}
		
		val binder = new WebDataBinder(new Object)
		
		val validator = mock[Validator]
		validator.supports(isA[Class[_]]) returns (true)
		
		binder.setValidator(validator)
		controller._binding(binder)
		
		binder.getValidator() match {
			case v: CompositeValidator => {
				v.list.length should be (2)
				v.list(0) should be (controller.validator)
				v.list(1) should be (validator)
			}
			case _ => fail()
		}
		binder.getDisallowedFields() should be (Array())
	}
	
	@Test def nukeOriginal {
		val command = new Object with SelfValidating {
			def validate(errors: Errors) {}
		}
		
		val controller = new BaseController {
			onlyValidatesWith[SelfValidating] { (obj: SelfValidating, errors) => }
		}
		
		controller.validator match {
			case _: ClassValidator[_] =>
			case _ => fail()
		}
		
		controller.validator.supports(classOf[SelfValidating]) should be (true)
		
		val binder = new WebDataBinder(command)
		
		val validator = mock[Validator]
		validator.supports(isA[Class[_]]) returns (true)
		
		binder.setValidator(validator)
		controller._binding(binder)
		
		// original validator was nuked by "onlyValidatesWith"
		binder.getValidator() should be (controller.validator)
		binder.getDisallowedFields() should be (Array())
	}
	
	@Test def disallowedFields {
		val controller = new BaseController {}
		controller.disallowedFields = List("steve", "yes")
		
		val binder = new WebDataBinder(new Object)
		controller._binding(binder)
		
		binder.getDisallowedFields() should be (Array("steve", "yes"))
	}
	
	@Test def enableFilters {
		val mockSession = mock[Session]
		val controller = new BaseController {
			override protected def session = mockSession
		}
		
		controller.showDeletedItems
		controller.preRequest
		there was no(mockSession).enableFilter("notDeleted")
		
		controller.hideDeletedItems
		controller.preRequest
		there was one(mockSession).enableFilter("notDeleted")
	}

}