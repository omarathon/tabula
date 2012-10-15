package uk.ac.warwick.courses.commands.assignments.extensions

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.{ Autowired, Configurable }
import uk.ac.warwick.courses.commands.{ Description, Command }
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.helpers.{ LazyLists, Logging }
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.services.UserLookupService
import reflect.BeanProperty
import org.springframework.transaction.annotation.Transactional

@Configurable
class DeleteExtensionCommand(val assignment: Assignment, val submitter: CurrentUser) extends Command[List[String]]
	with Daoisms with Logging {

	@Autowired var userLookup: UserLookupService = _
	@BeanProperty var universityIds: JList[String] = LazyLists.simpleFactory()

	@Transactional
	override def apply(): List[String] = {

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
