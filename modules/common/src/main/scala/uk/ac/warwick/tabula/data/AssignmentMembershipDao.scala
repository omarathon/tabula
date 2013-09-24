package uk.ac.warwick.tabula.data

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model._
import org.hibernate.criterion.{Order, Restrictions}
import uk.ac.warwick.tabula.AcademicYear
import org.springframework.stereotype.Repository

trait AssignmentMembershipDao {
	def find(assignment: AssessmentComponent): Option[AssessmentComponent]
	def find(group: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup]
	def find(group: AssessmentGroup): Option[AssessmentGroup]

	def save(group: AssessmentGroup): Unit
	def save(assignment: AssessmentComponent): AssessmentComponent
	def save(group: UpstreamAssessmentGroup)

	def delete(group: AssessmentGroup): Unit

	def getAssessmentGroup(id: String): Option[AssessmentGroup]
	def getUpstreamAssessmentGroup(id:String): Option[UpstreamAssessmentGroup]
	def getUpstreamAssignment(id: String): Option[AssessmentComponent]
	def getUpstreamAssignment(group: UpstreamAssessmentGroup): Option[AssessmentComponent]

	/**
	 * Get all UpstreamAssignments that appear to belong to this module.
	 *
	 *  Typically used to provide possible candidates to link to an app assignment,
	 *  in conjunction with #getUpstreamAssessmentGroups.
	 */
	def getUpstreamAssignments(module: Module): Seq[AssessmentComponent]
	def getUpstreamAssignments(department: Department): Seq[AssessmentComponent]

	/**
	 * Get all assessment groups that can serve this assignment this year.
	 * Should return as many groups as there are distinct OCCURRENCE values for a given
	 * assessment group code, which most of the time is just 1.
	 */
	def getUpstreamAssessmentGroups(upstreamAssignment: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]

	def countPublishedFeedback(assignment: Assignment): Int
	def countFullFeedback(assignment: Assignment): Int

	def getEnrolledAssignments(user: User): Seq[Assignment]
}

@Repository
class AssignmentMembershipDaoImpl extends AssignmentMembershipDao with Daoisms {
	def getEnrolledAssignments(user: User): Seq[Assignment] =
		session.newQuery[Assignment]("""select distinct a
			from Assignment a
			left join fetch a.assessmentGroups ag
			where
				(1 = (
					select 1 from uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup uag
					where uag.moduleCode = ag.assessmentComponent.moduleCode
						and uag.assessmentGroup = ag.assessmentComponent.assessmentGroup
						and uag.academicYear = a.academicYear
						and uag.occurrence = ag.occurrence
						and :universityId in elements(uag.members.staticIncludeUsers)
				) or :userId in elements(a.members.includeUsers))
				and :userId not in elements(a.members.excludeUsers)
				and a.deleted = false and a.archived = false""")
			.setString("universityId", user.getWarwickId)
			.setString("userId", user.getUserId)
			.seq

	/**
	 * Tries to find an identical AssessmentComponent in the database, based on the
	 * fact that moduleCode and sequence uniquely identify the assignment.
	 */
	def find(assignment: AssessmentComponent): Option[AssessmentComponent] = session.newCriteria[AssessmentComponent]
		.add(Restrictions.eq("moduleCode", assignment.moduleCode))
		.add(Restrictions.eq("sequence", assignment.sequence))
		.uniqueResult

	def find(group: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] = session.newCriteria[UpstreamAssessmentGroup]
		.add(Restrictions.eq("assessmentGroup", group.assessmentGroup))
		.add(Restrictions.eq("academicYear", group.academicYear))
		.add(Restrictions.eq("moduleCode", group.moduleCode))
		.add(Restrictions.eq("occurrence", group.occurrence))
		.uniqueResult

	def find(group: AssessmentGroup): Option[AssessmentGroup] = {
		val criteria = session.newCriteria[AssessmentGroup]
		.add(Restrictions.eq("assessmentComponent", group.assessmentComponent))
		.add(Restrictions.eq("occurrence", group.occurrence))

		if (group.assignment != null) {
			criteria.add(Restrictions.eq("assignment", group.assignment))
		} else {
			criteria.add(Restrictions.eq("smallGroupSet", group.smallGroupSet))
		}

		criteria.uniqueResult
	}

	def save(group:AssessmentGroup) = session.saveOrUpdate(group)

	def save(assignment: AssessmentComponent): AssessmentComponent =
		find(assignment)
			.map { existing =>
			if (existing needsUpdatingFrom assignment) {
				existing.copyFrom(assignment)
				session.update(existing)
			}

			existing
		}
			.getOrElse { session.save(assignment); assignment }

	def save(group: UpstreamAssessmentGroup) =
		find(group).getOrElse { session.save(group) }


	def getAssessmentGroup(id:String) = getById[AssessmentGroup](id)

	def getUpstreamAssessmentGroup(id:String) = getById[UpstreamAssessmentGroup](id)

	def delete(group: AssessmentGroup) {
		group.assignment.assessmentGroups.remove(group)
		session.delete(group)
		session.flush()
	}

	def getUpstreamAssignment(id: String) = getById[AssessmentComponent](id)

	def getUpstreamAssignment(group: UpstreamAssessmentGroup) = {
		session.newCriteria[AssessmentComponent]
			.add(Restrictions.eq("moduleCode", group.moduleCode))
			.add(Restrictions.eq("assessmentGroup", group.assessmentGroup))
			.uniqueResult
	}

	/** Just gets components of type Assignment for this module, not all components. */
	def getUpstreamAssignments(module: Module) = {
		session.newCriteria[AssessmentComponent]
			.add(Restrictions.like("moduleCode", module.code.toUpperCase + "-%"))
			.add(
				Restrictions.or(
					Restrictions.isNull("assessmentType"), // temporary, to handle legacy data (TAB-1174)
					Restrictions.eq("assessmentType", AssessmentType.Assignment)
				)
			 )
			.addOrder(Order.asc("sequence"))
			.seq filter isInteresting
	}

	/** Just gets components of type Assignment for this department, not all components. */
	def getUpstreamAssignments(department: Department) = {
		session.newCriteria[AssessmentComponent]
			.add(Restrictions.eq("departmentCode", department.code.toUpperCase))
			.add(
				Restrictions.or(
					Restrictions.isNull("assessmentType"), // temporary, to handle legacy data (TAB-1174)
					Restrictions.eq("assessmentType", AssessmentType.Assignment)
				)
			)
			.addOrder(Order.asc("moduleCode"))
			.addOrder(Order.asc("sequence"))
			.seq filter isInteresting
	}

	def countPublishedFeedback(assignment: Assignment): Int = {
		session.createSQLQuery("""select count(*) from feedback where assignment_id = :assignmentId and released = 1""")
			.setString("assignmentId", assignment.id)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	def countFullFeedback(assignment: Assignment): Int = {  //join f.attachments a
		session.createQuery("""select count(*) from Feedback f
			where f.assignment = :assignment
			and not (actualMark is null and actualGrade is null and f.attachments is empty)""")
			.setEntity("assignment", assignment)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	private def isInteresting(assignment: AssessmentComponent) = {
		!(assignment.name contains "NOT IN USE")
	}

	def getUpstreamAssessmentGroups(upstreamAssignment: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] = {
		session.newCriteria[UpstreamAssessmentGroup]
			.add(Restrictions.eq("academicYear", academicYear))
			.add(Restrictions.eq("moduleCode", upstreamAssignment.moduleCode))
			.add(Restrictions.eq("assessmentGroup", upstreamAssignment.assessmentGroup))
			.seq
	}
}
