package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.data.{AssignmentDaoComponent, AutowiringAssignmentDaoComponent}
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import org.joda.time.DateTime

trait AssignmentServiceComponent {
	def assignmentService: AssignmentService
}

trait AutowiringAssignmentServiceComponent extends AssignmentServiceComponent {
	var assignmentService = Wire[AssignmentService]
}

/**
 * Service providing access to Assignments and related objects.
 */
trait AssignmentService {
	def getAssignmentById(id: String): Option[Assignment]
	def save(assignment: Assignment): Unit

	def deleteFormField(field: FormField) : Unit

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment]

	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment]
	def getAssignmentsWithFeedback(studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment]

	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment]
	def getAssignmentsWithSubmission(studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment]

	def getSubmissionsForAssignmentsBetweenDates(universityId: String, startInclusive: DateTime, endExclusive: DateTime): Seq[Submission]

	def getAssignmentWhereMarker(user: User): Seq[Assignment]
	def getAssignmentsByDepartmentAndMarker(department: Department, user: CurrentUser): Seq[Assignment]
	def getAssignmentsByModuleAndMarker(module: Module, user: CurrentUser): Seq[Assignment]

	/**
	 * Find a recent assignment within this module or possible department.
	 */
	def recentAssignment(department: Department): Option[Assignment]

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment]

	def findAssignmentsByNameOrModule(query: String): Seq[Assignment]

	def filterAssignmentsByCourseAndYear(assignments: Seq[Assignment], studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment]

	def getAssignmentsClosingBetween(startInclusive: DateTime, endExclusive: DateTime): Seq[Assignment]

}

abstract class AbstractAssignmentService extends AssignmentService {
	self: AssignmentDaoComponent with AssignmentServiceUserGroupHelpers =>

	def getAssignmentById(id: String): Option[Assignment] = assignmentDao.getAssignmentById(id)
	def save(assignment: Assignment): Unit = assignmentDao.save(assignment)

	def deleteFormField(field: FormField) : Unit = assignmentDao.deleteFormField(field)

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment] =
		assignmentDao.getAssignmentByNameYearModule(name, year, module)

	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment] = assignmentDao.getAssignmentsWithFeedback(universityId)

	def getAssignmentsWithFeedback(studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment] = {
		val allAssignments = getAssignmentsWithFeedback(studentCourseYearDetails.studentCourseDetails.student.universityId)
		filterAssignmentsByCourseAndYear(allAssignments, studentCourseYearDetails)
	}

	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment] = assignmentDao.getAssignmentsWithSubmission(universityId)

	def getAssignmentsWithSubmission(studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment] = {
		val allAssignments = getAssignmentsWithSubmission(studentCourseYearDetails.studentCourseDetails.student.universityId)
		filterAssignmentsByCourseAndYear(allAssignments, studentCourseYearDetails)
	}

	def getSubmissionsForAssignmentsBetweenDates(universityId: String, startInclusive: DateTime, endExclusive: DateTime): Seq[Submission] =
		assignmentDao.getSubmissionsForAssignmentsBetweenDates(universityId, startInclusive, endExclusive)

	def getAssignmentWhereMarker(user: User): Seq[Assignment] = {
		(firstMarkerHelper.findBy(user) ++ secondMarkerHelper.findBy(user))
			.distinct
			.filterNot { a => a.deleted || a.archived }
	}

	def getAssignmentsByDepartmentAndMarker(department: Department, user: CurrentUser): Seq[Assignment] =
		getAssignmentWhereMarker(user.apparentUser).filter { _.module.adminDepartment == department }

	def getAssignmentsByModuleAndMarker(module: Module, user: CurrentUser): Seq[Assignment] =
		getAssignmentWhereMarker(user.apparentUser).filter { _.module == module }

	/**
	 * Find a recent assignment within this module or possible department.
	 */
	def recentAssignment(department: Department): Option[Assignment] = assignmentDao.recentAssignment(department)

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment] = assignmentDao.getAssignmentsByName(partialName, department)

	def findAssignmentsByNameOrModule(query: String): Seq[Assignment] = assignmentDao.findAssignmentsByNameOrModule(query)

	def filterAssignmentsByCourseAndYear(assignments: Seq[Assignment], studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment] = {
		assignments
			.filter(_.academicYear == studentCourseYearDetails.academicYear)
			.filter(assignment => {
				val allStudentsModulesForYear = studentCourseYearDetails.studentCourseDetails.student.registeredModulesByYear(Some(assignment.academicYear))

				// include the assignment only for the course with the relevant module registration -
				// unless the student isn't registered on the module at all, in which case include this assignment under all the student's course tabs
				studentCourseYearDetails.registeredModules.contains(assignment.module) || !allStudentsModulesForYear.contains(assignment.module)
			}
		)
	}

	def getAssignmentsClosingBetween(start: DateTime, end: DateTime) = assignmentDao.getAssignmentsClosingBetween(start, end)
}

trait AssignmentServiceUserGroupHelpers {
	val firstMarkerHelper: UserGroupMembershipHelper[Assignment]
	val secondMarkerHelper: UserGroupMembershipHelper[Assignment]
}

trait AssignmentServiceUserGroupHelpersImpl extends AssignmentServiceUserGroupHelpers {
	val firstMarkerHelper = new UserGroupMembershipHelper[Assignment]("markingWorkflow._firstMarkers")
	val secondMarkerHelper = new UserGroupMembershipHelper[Assignment]("markingWorkflow._secondMarkers")
}

@Service(value = "assignmentService")
class AssignmentServiceImpl
	extends AbstractAssignmentService
	with AutowiringAssignmentDaoComponent
	with AssignmentServiceUserGroupHelpersImpl

