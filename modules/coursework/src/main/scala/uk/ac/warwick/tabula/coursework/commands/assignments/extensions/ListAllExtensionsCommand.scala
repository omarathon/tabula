package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.{Department, Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.services.{AssignmentService, UserLookupService, AssignmentMembershipService}
import uk.ac.warwick.tabula.data.model.forms.{ExtensionState, Extension}
import uk.ac.warwick.userlookup.User
import org.joda.time.{Days, DateTime}
import uk.ac.warwick.tabula.coursework.web.Routes.admin.assignment.extension
import uk.ac.warwick.tabula.data.AssignmentDao

class ListAllExtensionsCommand(val department: Department, val user: CurrentUser)
	extends Command[Seq[ExtensionGraph]] with ReadOnly with Unaudited {

	PermissionCheck(Permissions.Extension.Read, department)

	var userLookup = Wire.auto[UserLookupService]
	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]
	var assignmentDao = Wire.auto[AssignmentDao]

	def applyInternal(): Seq[ExtensionGraph] = {

		val year = AcademicYear.guessByDate(new DateTime())

		// get all the users that aren't members of an assignment in this dept, but have submitted work to one

		val allExtensions = (for (assignment <- assignmentDao.getAssignments(department, year)) yield {
			assignment.extensions.asScala.filter(ext => ext.awaitingReview || ext.approved || ext.rejected)
		}).flatten

		for (extension <- allExtensions) yield {
			getExtensionGraphFromExtension(extension)
		}
	}

	def getExtensionGraphFromExtension(extension: Extension): ExtensionGraph = {
		new ExtensionGraph(
			extension.universityId,
			userLookup.getUserByWarwickUniId(extension.universityId),
			extension.awaitingReview,
			extension.approved,
			extension.rejected,
			extension.duration,
			extension.requestedExtraDuration,
			Some(extension))
	}

}
