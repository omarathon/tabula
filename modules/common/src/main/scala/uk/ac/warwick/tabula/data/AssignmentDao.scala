package uk.ac.warwick.tabula.data

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, Module, Assignment}
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.forms.FormField
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.AcademicYear
import org.hibernate.criterion.{Order, Restrictions}

trait AssignmentDaoComponent {
	val assignmentDao: AssignmentDao
}

trait AutowiringAssignmentDaoComponent extends AssignmentDaoComponent {
	val assignmentDao = Wire[AssignmentDao]
}

trait AssignmentDao {

	def getAssignmentById(id: String): Option[Assignment]
	def save(assignment: Assignment): Unit

	def deleteFormField(field: FormField): Unit

	def getAssignmentsWithFeedback(universityId: String): Seq[Assignment]
	def getAssignmentsWithSubmission(universityId: String): Seq[Assignment]
	def getAssignmentsWithSubmissionBetweenDates(universityId: String, start: DateTime, end: DateTime): Seq[Assignment]

	def getAssignmentWhereMarker(user: User): Seq[Assignment]
	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment]
	def recentAssignment(department: Department): Option[Assignment]

	val MaxAssignmentsByName = 15

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment]

	def findAssignmentsByNameOrModule(query: String): Seq[Assignment]
}

@Repository
class AssignmentDaoImpl extends AssignmentDao with Daoisms {

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

	def getAssignmentsWithSubmissionBetweenDates(universityId: String, start: DateTime, end: DateTime): Seq[Assignment] =
		Seq()

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
		session.newCriteria[Assignment]
			.createAlias("module", "m")
			.add(is("m.department", department))
			.add(Restrictions.isNotNull("createdDate"))
			.addOrder(Order.desc("createdDate"))
			.setMaxResults(1)
			.uniqueResult
	}

	def getAssignmentsByName(partialName: String, department: Department) = {

		session.newQuery[Assignment]("""select a from Assignment a
				where a.module.department = :dept
				and a.name like :nameLike
				order by createdDate desc
																 """)
			.setParameter("dept", department)
			.setString("nameLike", "%" + partialName + "%")
			.setMaxResults(MaxAssignmentsByName).seq
	}

	def findAssignmentsByNameOrModule(query: String) = {
		session.newQuery[Assignment]("""select a from Assignment
				a where a.name like :nameLike
				or a.module.code like :nameLike
				order by createdDate desc
																 """)
			.setString("nameLike", "%" + query + "%")
			.setMaxResults(MaxAssignmentsByName).seq
	}

}