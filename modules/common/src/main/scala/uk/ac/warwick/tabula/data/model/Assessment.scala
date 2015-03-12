package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentMembershipInfo}
import collection.JavaConverters._

trait Assessment extends GeneratedId with CanBeDeleted with PermissionsTarget {

	@transient
	var assessmentMembershipService = Wire[AssessmentMembershipService]("assignmentMembershipService")

	var module: Module
	var academicYear: AcademicYear
	var name: String
	var assessmentGroups: JList[AssessmentGroup]
	def addDefaultFeedbackFields(): Unit
	def addDefaultFields(): Unit

	def allFeedback: Seq[Feedback]

	// feedback that has been been through the marking process (not placeholders for marker feedback)
	def fullFeedback = allFeedback.filterNot(_.isPlaceholder).toSeq

	// returns feedback for a specified student
	def findFeedback(uniId: String) = allFeedback.find(_.universityId == uniId)

	// returns feedback for a specified student
	def findFullFeedback(uniId: String) = fullFeedback.find(_.universityId == uniId)


	// converts the assessmentGroups to upstream assessment groups
	def upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup] =
		assessmentGroups.asScala.flatMap {
			_.toUpstreamAssessmentGroup(academicYear)
		}

	// Gets a breakdown of the membership for this assessment. Note that this cannot be sorted by seat number
	def membershipInfo: AssessmentMembershipInfo = assessmentMembershipService.determineMembership(this)

	def collectMarks: JBoolean

	def hasWorkflow: Boolean


}
