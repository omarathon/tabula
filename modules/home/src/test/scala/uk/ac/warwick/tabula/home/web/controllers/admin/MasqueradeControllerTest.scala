package uk.ac.warwick.tabula.home.web.controllers.admin

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.home.commands.admin.MasqueradeCommand
import uk.ac.warwick.tabula.MockUserLookup
import org.springframework.mock.web.MockHttpServletResponse
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.events.EventHandling

class MasqueradeControllerTest extends TestBase {
	
	val userLookup = new MockUserLookup
	userLookup.registerUsers("cusebr")
	
	val controller = new MasqueradeController
	
	EventHandling.enabled = false
	
	@Test def add {
		val cmd = new MasqueradeCommand
		cmd.userLookup = userLookup
		
		cmd.usercode = "cusebr"

		val response = new MockHttpServletResponse
		controller.submit(cmd, response).viewName should be ("redirect:/admin/masquerade")
		
		response.getCookies().length should be (1)
		response.getCookie(CurrentUser.masqueradeCookie).getValue should be ("cusebr")
	}
	
	@Test def remove {
		val cmd = new MasqueradeCommand
		cmd.userLookup = userLookup
		
		cmd.action = "remove"
			
		val response = new MockHttpServletResponse
		controller.submit(cmd, response).viewName should be ("redirect:/admin/masquerade")
		
		response.getCookies().length should be (1)
		response.getCookie(CurrentUser.masqueradeCookie).getValue should be (null)
	}
	
	@Test def noop {
		val cmd = new MasqueradeCommand
		cmd.userLookup = userLookup
		
		cmd.usercode = "undefined"
			
		val response = new MockHttpServletResponse
		controller.submit(cmd, response).viewName should be ("redirect:/admin/masquerade")
		
		response.getCookies().length should be (0)
		response.getCookie(CurrentUser.masqueradeCookie) should be (null)
	}

}