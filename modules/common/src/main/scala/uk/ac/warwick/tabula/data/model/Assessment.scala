package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import collection.JavaConverters._

trait Assessment extends GeneratedId with CanBeDeleted with PermissionsTarget {

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
}
