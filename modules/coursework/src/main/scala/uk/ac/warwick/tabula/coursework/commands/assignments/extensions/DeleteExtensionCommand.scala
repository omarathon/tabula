package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.{Notifies, Description, Command}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.{ LazyLists, Logging }
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications.{ExtensionRevokedNotification, ExtensionChangedNotification}
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer


class DeleteExtensionCommand(val module: Module, val assignment: Assignment, val universityId: String, val submitter: CurrentUser) extends Command[Seq[Extension]]
	with Notifies[Seq[Extension], Option[Extension]] with Daoisms with Logging {
	
	var universityIds: JList[String] = LazyLists.create()

	universityIds.add(universityId)

	mustBeLinked(assignment,module)
	PermissionCheck(Permissions.Extension.Delete, module)

	var userLookup = Wire.auto[UserLookupService]
	
	override def applyInternal(): Seq[Extension] = transactional() {

		// return false if no extension exists for the given ID. Otherwise deletes that extension and returns true
		def deleteExtension(universityId: String): Option[Extension] = {
			val extension = assignment.findExtension(universityId)
			extension.foreach(extension =>{
				extension.assignment.extensions.remove(extension)
				session.delete(extension)
			})
			extension
		}

		// return the IDs of all the deleted extensions
		universityIds.flatMap(deleteExtension)
	}

	def describe(d: Description) {
		d.assignment(assignment)
		 .studentIds(universityIds)
	}
	
	override def describeResult(d: Description, extensions: Seq[Extension]) {
		d.assignment(assignment)
		 .studentIds(universityIds)
		 .property("extension" -> extensions.map { _.id })
		 .fileAttachments(extensions.flatMap { _.attachments })
	}

	def emit(extensions: Seq[Extension]) = {
		extensions.map { extension =>
			val student = userLookup.getUserByWarwickUniId(extension.universityId)
			new ExtensionRevokedNotification(assignment, student, submitter.apparentUser) with FreemarkerTextRenderer
		}
	}
}
