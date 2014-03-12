package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.data.{AssignmentDaoComponent, AutowiringAssignmentDaoComponent}
import uk.ac.warwick.tabula.AcademicYear
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
	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment]
	def getSubmissionsForAssignmentsBetweenDates(universityId: String, startInclusive: DateTime, endExclusive: DateTime): Seq[Submission]

	def getAssignmentWhereMarker(user: User): Seq[Assignment]

	/**
	 * Find a recent assignment within this module or possible department.
	 */
	def recentAssignment(department: Department): Option[Assignment]

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment]

	def findAssignmentsByNameOrModule(query: String): Seq[Assignment]

}

abstract class AbstractAssignmentService extends AssignmentService {
	self: AssignmentDaoComponent =>

	def getAssignmentById(id: String): Option[Assignment] = assignmentDao.getAssignmentById(id)
	def save(assignment: Assignment): Unit = assignmentDao.save(assignment)

	def deleteFormField(field: FormField) : Unit = assignmentDao.deleteFormField(field)

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment] =
		assignmentDao.getAssignmentByNameYearModule(name, year, module)

	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment] = assignmentDao.getAssignmentsWithFeedback(universityId)
	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment] = assignmentDao.getAssignmentsWithSubmission(universityId)
	def getSubmissionsForAssignmentsBetweenDates(universityId: String, startInclusive: DateTime, endExclusive: DateTime): Seq[Submission] =
		assignmentDao.getSubmissionsForAssignmentsBetweenDates(universityId, startInclusive, endExclusive)

	def getAssignmentWhereMarker(user: User): Seq[Assignment] = assignmentDao.getAssignmentWhereMarker(user)

	/**
	 * Find a recent assignment within this module or possible department.
	 */
	def recentAssignment(department: Department): Option[Assignment] = assignmentDao.recentAssignment(department)

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment] = assignmentDao.getAssignmentsByName(partialName, department)

	def findAssignmentsByNameOrModule(query: String): Seq[Assignment] = assignmentDao.findAssignmentsByNameOrModule(query)
}

@Service(value = "assignmentService")
class AssignmentServiceImpl
	extends AbstractAssignmentService
	with AutowiringAssignmentDaoComponent

