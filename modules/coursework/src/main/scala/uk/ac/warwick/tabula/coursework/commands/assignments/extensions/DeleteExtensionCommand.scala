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


class DeleteExtensionCommand(val module: Module, val assignment: Assignment, val universityId: String, val submitter: CurrentUser) extends Command[Seq[String]]
	with Notifies[Seq[String], Option[Extension]] with Daoisms with Logging {
	
	var universityIds: JList[String] = LazyLists.create()

	universityIds.add(universityId)

	mustBeLinked(assignment,module)
	PermissionCheck(Permissions.Extension.Delete, module)

	var userLookup = Wire.auto[UserLookupService]
	
	override def applyInternal(): Seq[String] = transactional() {

		// return false if no extension exists for the given ID. Otherwise deletes that extension and returns true
		def deleteExtension(universityId: String): Boolean = {
			val extensions = assignment.findExtension(universityId)
			extensions.foreach(extension =>{
				extension.assignment.extensions.remove(extension)
				session.delete(extension)
			})
			extensions.isDefined
		}

		// return the IDs of all the deleted extensions
		universityIds = universityIds.filter(deleteExtension(_))
		universityIds.toSeq
	}

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(assignment.module)
		d.studentIds(universityIds)
	}

	def emit(universityIds: Seq[String]) = {
		universityIds.map(studentId => {
			val student = userLookup.getUserByWarwickUniId(studentId)
			new ExtensionRevokedNotification(assignment, student, submitter.apparentUser) with FreemarkerTextRenderer
		})
	}
}
