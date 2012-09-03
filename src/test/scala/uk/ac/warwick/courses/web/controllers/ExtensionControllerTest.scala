package uk.ac.warwick.courses.web.controllers

import scala.collection.JavaConversions._
import uk.ac.warwick.courses.web.controllers.admin.ExtensionController
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.services.{UserLookupService, AssignmentService}
import uk.ac.warwick.courses.{Mockito, TestBase}
import uk.ac.warwick.courses.commands.assignments.{ExtensionItem, EditExtensionCommand}
import org.joda.time.DateTime
import uk.ac.warwick.courses.data.model.forms.Extension


class ExtensionControllerTest extends TestBase with Mockito {

  @Test def returnJson(){
    withUser("cuslaj") {
      val controller = new ExtensionController()
      controller.assignmentService = mock[AssignmentService]
      controller.json = json

      val assignment = newDeepAssignment()
      assignment.closeDate = DateTime.parse("2012-08-15T12:00")
      assignment.extensions += new Extension("1170836")

      val command = new EditExtensionCommand(assignment, currentUser)
      command.userLookup = mock[UserLookupService]
      command.extensionItems = mockExtensions

      val extensions = command.copyExtensions()
      val extensionsJson:JList[Map[String, String]] = controller.toJson("Test", extensions) toList
      val string = json.writeValueAsString(extensionsJson)
      string should be ("""[{"id":"1170836","expiryDate":"Thu 23rd August 2012 at 12:00","reason":"Donec a risus purus nullam.","action":"Test"}]""")
    }
  }

  def mockExtensions = {
    val extensionItem = new ExtensionItem
    extensionItem.universityId = "1170836"
    extensionItem.expiryDate = DateTime.parse("2012-08-23T12:00")
    extensionItem.reason = "Donec a risus purus nullam."
    List(extensionItem)
  }
}
