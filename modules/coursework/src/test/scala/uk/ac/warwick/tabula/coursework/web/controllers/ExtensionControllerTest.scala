package uk.ac.warwick.tabula.coursework.web.controllers

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.web.controllers.admin.ExtensionController
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.{Mockito, TestBase}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.{ExtensionItem, ModifyExtensionCommand}
import uk.ac.warwick.tabula.coursework.web.controllers.admin.AddExtensionController
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.AddExtensionCommand


class ExtensionControllerTest extends TestBase with Mockito {

  @Test def returnJson(){
    withUser("cuslaj") {
      val controller = new AddExtensionController()
      controller.json = json

      val assignment = newDeepAssignment()
      assignment.closeDate = DateTime.parse("2012-08-15T12:00")
      assignment.extensions += new Extension("1170836")

      val command = new AddExtensionCommand(assignment.module, assignment, currentUser)
      command.userLookup = mock[UserLookupService]
      command.extensionItems = mockExtensions

      val extensions = command.copyExtensionItems()
      val extensionsJson = controller.toJson(extensions)
      val string = json.writeValueAsString(extensionsJson)
      string should be ("""{"1170836":{"id":"1170836","status":"","expiryDate":"12:00&#8194;Thu 23<sup>rd</sup> August 2012","approvalComments":"Donec a risus purus nullam."}}""")
    }
  }

  def mockExtensions = {
    val extensionItem = new ExtensionItem
    extensionItem.universityId = "1170836"
    extensionItem.expiryDate = DateTime.parse("2012-08-23T12:00")
    extensionItem.approvalComments = "Donec a risus purus nullam."
    List(extensionItem)
  }
}
