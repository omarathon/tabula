package uk.ac.warwick.tabula.services

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.persistence.Entity
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.userlookup.User
import org.hibernate.criterion.{Projections, Restrictions, Order}
import uk.ac.warwick.tabula.helpers.{ FoundUser, Logging }
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.AssessmentGroup
import uk.ac.warwick.spring.Wire

/**
 * Service providing access to Assignments and related objects.
 */
trait AssignmentService {
	def getAssignmentById(id: String): Option[Assignment]
	def save(assignment: Assignment)

	def deleteFormField(field: FormField) : Unit

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment]

	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment]
	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment]

	def getAssignmentWhereMarker(user: User): Seq[Assignment]

	/**
	 * Find a recent assignment within this module or possible department.
	 */
	def recentAssignment(department: Department): Option[Assignment]

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment]
}



@Service(value = "assignmentService")
class AssignmentServiceImpl
	extends AssignmentService
		with Daoisms
		with Logging {
	import Restrictions._

	@Autowired var auditEventIndexService: AuditEventIndexService = _

	def getAssignmentById(id: String) = getById[Assignment](id)
	def save(assignment: Assignment) = session.saveOrUpdate(assignment)

	def deleteFormField(field: FormField) {
		session.delete(field)
	}

	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment] =
		session.newQuery[Assignment]("""select a from Assignment a
				join a.feedbacks as f
				where f.universityId = :universityId
				and f.released=true""")
			.setString("universityId", universityId)
			.distinct.seq

	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment] =
		session.newQuery[Assignment]("""select a from Assignment a
				join a.submissions as f
				where f.universityId = :universityId""")
			.setString("universityId", universityId)
			.distinct.seq

	def getAssignmentWhereMarker(user: User): Seq[Assignment] =
		session.newQuery[Assignment]("""select a
				from Assignment a
				where (:userId in elements(a.markingWorkflow.firstMarkers.includeUsers)
					or :userId in elements(a.markingWorkflow.secondMarkers.includeUsers))
					and a.deleted = false and a.archived = false
																 """).setString("userId", user.getUserId).distinct.seq

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module) =
		session.newQuery[Assignment]("from Assignment where name=:name and academicYear=:year and module=:module and deleted=0")
			.setString("name", name)
			.setParameter("year", year)
			.setEntity("module", module)
			.seq

	def recentAssignment(department: Department) = {
		//auditEventIndexService.recentAssignment(department)
		session.newCriteria[Assignment]
			.createAlias("module", "m")
			.add(is("m.department", department))
			.add(Restrictions.isNotNull("createdDate"))
			.addOrder(Order.desc("createdDate"))
			.setMaxResults(1)
			.uniqueResult
	}

	val MaxAssignmentsByName = 15

	def getAssignmentsByName(partialName: String, department: Department) = {
		session.newCriteria[Assignment]
			.createAlias("module", "mod")
			.add(is("mod.department", department))
			.add(Restrictions.ilike("name", "%" + partialName + "%"))
			.addOrder(Order.desc("createdDate"))
			.setMaxResults(MaxAssignmentsByName)
			.list
	}
}

trait AssignmentServiceComponent {
	def assignmentService: AssignmentService
}

trait AutowiringAssignmentServiceComponent extends AssignmentServiceComponent{
	var assignmentService = Wire[AssignmentService]
}

