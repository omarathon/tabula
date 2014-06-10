package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.AssignmentDao
import uk.ac.warwick.tabula.coursework.helpers.ExtensionGraph

class ListAllExtensionsCommand(val department: Department)
	extends Command[Seq[ExtensionGraph]] with ReadOnly with Unaudited {

	// This permissions check limits this to anyone who has extension read permission over the whole department.
	// Since extension manager is only available as a departmental role ATOW, that's OK for now.
	// If extension manager were to become more fine-grained in future and used for people who don't have
	// Extension.Read permission, we would need to make this less strict.
	PermissionCheck(Permissions.Extension.Read, mandatory(department))

	var assignmentDao = Wire.auto[AssignmentDao]

	def applyInternal(): Seq[ExtensionGraph] = {

		val year = AcademicYear.guessByDate(new DateTime())

		// get all extensions for assignments in modules in the department for the current year
		assignmentDao.getAssignments(department, year)
			.flatMap { _.extensions.asScala }
			.map(ExtensionGraph(_))
	}

}
