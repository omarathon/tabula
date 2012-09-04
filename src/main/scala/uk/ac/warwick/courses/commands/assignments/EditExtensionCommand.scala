package uk.ac.warwick.courses.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.data.model.forms.Extension
import uk.ac.warwick.courses.commands.Description

class EditExtensionCommand(val assignment:Assignment, val submitter: CurrentUser) extends ModifyExtensionCommand {

  def copyFromExtension(extension:Extension) = {
    val item:ExtensionItem = new ExtensionItem
    item.universityId =  extension.universityId
    item.reason = extension.reason
    item.expiryDate = extension.expiryDate

    extensionItems += item
  }

  def describe(d: Description) {
    d.assignment(assignment)
    d.module(assignment.module)
    d.studentIds(extensionItems map (_.universityId))
  }

}
