package uk.ac.warwick.tabula.admin.web.controllers

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.admin.commands.{MasqueradeCommandPermissions, MasqueradeCommandState}
import uk.ac.warwick.tabula.commands.{Describable, Appliable}
import uk.ac.warwick.tabula.web.Cookie
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.web.Cookies._

class MasqueradeControllerTest extends TestBase with Mockito {
	
	val controller = new MasqueradeController

	@Test def createCommand {
		val command = controller.command()

		command should be (anInstanceOf[Appliable[Option[Cookie]]])
		command should be (anInstanceOf[MasqueradeCommandState])
		command should be (anInstanceOf[MasqueradeCommandPermissions])
		command should be (anInstanceOf[Describable[Option[Cookie]]])
	}

	@Test def form {
		controller.form(mock[Appliable[Option[Cookie]]]).viewName should be ("masquerade/form")
	}

	@Test def submit {
		val cookie = mock[Cookie]
		val command = mock[Appliable[Option[Cookie]]]
		command.apply() returns (Some(cookie))

		val response = mock[HttpServletResponse]

		controller.submit(command, response).viewName should be ("redirect:/masquerade")
		there was one (response).addCookie(cookie)
	}

}