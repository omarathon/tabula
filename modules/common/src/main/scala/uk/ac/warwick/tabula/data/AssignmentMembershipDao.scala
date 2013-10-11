package uk.ac.warwick.tabula.data

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model._
import org.hibernate.criterion.{Order, Restrictions}
import uk.ac.warwick.tabula.AcademicYear
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet

trait AssignmentMembershipDaoComponent {
	val membershipDao: AssignmentMembershipDao
}

trait AutowiringAssignmentMembershipDaoComponent extends AssignmentMembershipDaoComponent {
	val membershipDao = Wire[AssignmentMembershipDao]
}

/**
 * TODO Rename all of this to be less Assignment-centric
 */
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
	def getAssessmentComponent(id: String): Option[AssessmentComponent]
	def getAssessmentComponent(group: UpstreamAssessmentGroup): Option[AssessmentComponent]

	/**
	 * Get all AssessmentComponents that appear to belong to this module.
	 *
	 *  Typically used to provide possible candidates to link to an app assignment,
	 *  in conjunction with #getUpstreamAssessmentGroups.
	 */
	def getAssessmentComponents(module: Module): Seq[AssessmentComponent]
	def getAssessmentComponents(department: Department): Seq[AssessmentComponent]

	/**
	 * Get all assessment groups that can serve this assignment this year.
	 * Should return as many groups as there are distinct OCCURRENCE values for a given
	 * assessment group code, which most of the time is just 1.
	 */
	def getUpstreamAssessmentGroups(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup]

	def countPublishedFeedback(assignment: Assignment): Int
	def countFullFeedback(assignment: Assignment): Int

	def getEnrolledAssignments(user: User): Seq[Assignment]
	def getEnrolledSmallGroupSets(user: User): Seq[SmallGroupSet]
}

@Repository
class AssignmentMembershipDaoImpl extends AssignmentMembershipDao with Daoisms {

	def getEnrolledAssignments(user: User): Seq[Assignment] =
		session.newQuery[Assignment]("""select a
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
			.distinct.seq
			
	def getEnrolledSmallGroupSets(user: User): Seq[SmallGroupSet] =
		session.newQuery[SmallGroupSet]("""select sgs
			from SmallGroupSet sgs
			left join fetch sgs.assessmentGroups ag
			where
				(1 = (
					select 1 from uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup uag
					where uag.moduleCode = ag.assessmentComponent.moduleCode
						and uag.assessmentGroup = ag.assessmentComponent.assessmentGroup
						and uag.academicYear = sgs.academicYear
						and uag.occurrence = ag.occurrence
						and :universityId in elements(uag.members.staticIncludeUsers)
				) or (
					(sgs._membersGroup.universityIds = false and :userId in elements(sgs._membersGroup.includeUsers)) or
					(sgs._membersGroup.universityIds = true and :universityId in elements(sgs._membersGroup.includeUsers))
				))
				and (
					(sgs._membersGroup.universityIds = false and :userId not in elements(sgs._membersGroup.excludeUsers)) or
					(sgs._membersGroup.universityIds = true and :universityId not in elements(sgs._membersGroup.excludeUsers))
				)
				and sgs.deleted = false and sgs.archived = false""")
			.setString("universityId", user.getWarwickId)
			.setString("userId", user.getUserId)
			.distinct.seq

	/**
	 * Tries to find an identical AssessmentComponent in the database, based on the
	 * fact that moduleCode and sequence uniquely identify the assignment.
	 */
	def find(assignment: AssessmentComponent): Option[AssessmentComponent] = session.newCriteria[AssessmentComponent]
		.add(is("moduleCode", assignment.moduleCode))
		.add(is("sequence", assignment.sequence))
		.uniqueResult

	def find(group: UpstreamAssessmentGroup): Option[UpstreamAssessmentGroup] = session.newCriteria[UpstreamAssessmentGroup]
		.add(is("assessmentGroup", group.assessmentGroup))
		.add(is("academicYear", group.academicYear))
		.add(is("moduleCode", group.moduleCode))
		.add(is("occurrence", group.occurrence))
		.uniqueResult

	def find(group: AssessmentGroup): Option[AssessmentGroup] = {
		if (group.assignment == null && group.smallGroupSet == null) None
		else {
			val criteria = session.newCriteria[AssessmentGroup]
			.add(is("assessmentComponent", group.assessmentComponent))
			.add(is("occurrence", group.occurrence))
	
			if (group.assignment != null) {
				criteria.add(is("assignment", group.assignment))
			} else {
				criteria.add(is("smallGroupSet", group.smallGroupSet))
			}
	
			criteria.uniqueResult
		}
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

	def getAssessmentComponent(id: String) = getById[AssessmentComponent](id)

	def getAssessmentComponent(group: UpstreamAssessmentGroup) = {
		session.newCriteria[AssessmentComponent]
			.add(is("moduleCode", group.moduleCode))
			.add(is("assessmentGroup", group.assessmentGroup))
			.uniqueResult
	}

	/** Just gets components of type Assignment for this module, not all components. */
	def getAssessmentComponents(module: Module) = {
		session.newCriteria[AssessmentComponent]
			.add(Restrictions.like("moduleCode", module.code.toUpperCase + "-%"))
			.addOrder(Order.asc("sequence"))
			.seq filter isInteresting
	}

	/** Just gets components of type Assignment for this department, not all components. */
	def getAssessmentComponents(department: Department) = {
		session.newCriteria[AssessmentComponent]
			.add(is("departmentCode", department.code.toUpperCase))
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

	def getUpstreamAssessmentGroups(component: AssessmentComponent, academicYear: AcademicYear): Seq[UpstreamAssessmentGroup] = {
		session.newCriteria[UpstreamAssessmentGroup]
			.add(is("academicYear", academicYear))
			.add(is("moduleCode", component.moduleCode))
			.add(is("assessmentGroup", component.assessmentGroup))
			.seq
	}
}
