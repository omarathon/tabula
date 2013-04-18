package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.{ Description, Command }
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.{ LazyLists, Logging }
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.UserLookupService
import reflect.BeanProperty
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions._


class DeleteExtensionCommand(val module: Module, val assignment: Assignment, val universityId: String, val submitter: CurrentUser) extends Command[List[String]]
	with Daoisms with Logging {
	
	var universityIds: JList[String] = LazyLists.simpleFactory()

	universityIds.add(universityId)

	mustBeLinked(assignment,module)
	PermissionCheck(Permissions.Extension.Delete, module)

	var userLookup = Wire[UserLookupService]
	
	override def applyInternal(): List[String] = transactional() {

		// return false if no extension exists for the given ID. Otherwise deletes that extension and returns true
		def deleteExtension(universityId: String): Boolean = {
			val extension = assignment.findExtension(universityId).getOrElse({
				return false
			})
			extension.assignment.extensions.remove(extension)
			session.delete(extension)
			true
		}

		// return the IDs of all the deleted extensions
		universityIds = universityIds.filter(deleteExtension(_))
		universityIds.toList
	}

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(assignment.module)
		d.studentIds(universityIds)
	}
}
