package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.JsonObjectMapperFactory
import uk.ac.warwick.tabula.MockUserLookup
import java.io.StringWriter

class UserPickerControllerTest extends TestBase {
	
	val controller = new UserPickerController
	controller.json = new JsonObjectMapperFactory().createInstance 
	
	@Test def queryJson {
		val cmd = new UserPickerController.UserPickerCommand()
		
		val userLookup = new MockUserLookup
		userLookup.registerUsers("cuscav", "cusebr")
		
		userLookup.addFindUsersWithFilterResult(userLookup.getUserByUserId("cuscav"))
		userLookup.addFindUsersWithFilterResult(userLookup.getUserByUserId("cusebr"))
		userLookup.findUsersEnabled = true
		
		cmd.userLookup = userLookup
		
		val writer = new StringWriter
		controller.queryJson(cmd, writer)
		
		val users = controller.json.readValue(writer.getBuffer().toString(), classOf[Seq[java.util.LinkedHashMap[String, Any]]])
		users.length should be (2)
		
		// Ensure order has been retained
		users(0).get("value") should be ("cuscav")
		users(1).get("value") should be ("cusebr")
	}

}