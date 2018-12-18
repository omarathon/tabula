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
import org.hibernate.Session

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
			override protected def session: Session = mockSession
		}

		controller.showDeletedItems
		controller.preRequest
		verify(mockSession, times(0)).enableFilter("notDeleted")

		controller.hideDeletedItems
		controller.preRequest
		verify(mockSession, times(1)).enableFilter("notDeleted")
	}

	@Test def sanitiseReturnTo(): Unit = {
		val controller = new BaseController {}
		val badPath1 = "javascript:alert(1)" // not safe at all, should be killed
		controller.getReturnTo(badPath1) should be ("")

		val badPath2 = "/javascript:alert(1)" // looks bad, but actually valid
		controller.getReturnTo(badPath2) should be ("/javascript:alert(1)")

		val badPath3 = "%20javascript:alert(1)" // crashes URI
		controller.getReturnTo(badPath3) should be ("")

		val badPath4 = "https://blah/wara" // we dont want any scheme
		controller.getReturnTo(badPath4) should be ("")

		val badPath5 = "%20javascript:alert(1)" // crashes URI
		controller.getReturnTo(badPath5) should be ("")

		val badPath7 = "https://ss.co/this/is/what"
		controller.getReturnTo(badPath7) should be ("")

		val badPath8 = "https://ss.co/this/is/what"
		controller.getReturnTo(badPath8) should be ("")

		val badPath9 = s"""\"\""://ss.co/this/is/what"""
		controller.getReturnTo(badPath9) should be ("")

		val badPath10 = s"""\"strange\""://ss.co/this/is/what"""
		controller.getReturnTo(badPath10) should be ("")

		val badPath11 = s"://ss.co/this/is/what"
		controller.getReturnTo(badPath11) should be ("")

		val goodPath1 = "/i/eat/chocolate" // good one
		controller.getReturnTo(goodPath1) should be ("/i/eat/chocolate")

		val goodPath2 = "this/is/what"
		controller.getReturnTo(goodPath2) should be ("this/is/what")

		val goodPath3 = "/assignment?teacher=duck&room=[1,2,3]"
		controller.getReturnTo(goodPath3) should be ("/assignment?teacher=duck&room=[1,2,3]")

		val goodPath4 = "/assignment?teacher=duck"
		controller.getReturnTo(goodPath4) should be ("/assignment?teacher=duck")

		val emptyString = ""
		controller.getReturnTo(emptyString) should be ("")

	}

}