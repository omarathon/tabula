package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{AssessmentMembershipInfo, AssessmentMembershipService}

import scala.jdk.CollectionConverters._

trait Assessment extends GeneratedId with CanBeDeleted with PermissionsTarget {

  @transient
  var assessmentMembershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]("assignmentMembershipService")
  var module: Module
  var academicYear: AcademicYear
  var name: String
  var assessmentGroups: JList[AssessmentGroup]

  def addDefaultFeedbackFields(): Unit

  def addDefaultFields(): Unit

  def allFeedback: Seq[Feedback]

  // feedback that has been been through the marking process (not placeholders for marker feedback)
  def fullFeedback: Seq[Feedback] = allFeedback.filterNot(_.isPlaceholder)

  // returns feedback for a specified student
  def findFeedback(usercode: String): Option[Feedback] = allFeedback.find(_.usercode == usercode)

  // returns feedback for a specified student
  def findFullFeedback(usercode: String): Option[Feedback] = fullFeedback.find(_.usercode == usercode)

  // converts the assessmentGroups to UpstreamAssessmentGroupInfo
  def upstreamAssessmentGroupInfos: Seq[UpstreamAssessmentGroupInfo] = RequestLevelCache.cachedBy("Assessment.upstreamAssessmentGroupInfos", id) {
    assessmentMembershipService.getUpstreamAssessmentGroupInfo(assessmentGroups.asScala.toSeq, academicYear)
  }

  // Gets a breakdown of the membership for this assessment. Note that this cannot be sorted by seat number
  def membershipInfo: AssessmentMembershipInfo = RequestLevelCache.cachedBy("Assessment.membershipInfo", id) {
    assessmentMembershipService.determineMembership(this)
  }

  def collectMarks: JBoolean

  // if any feedback exists that has a marker feedback with a marker then at least one marker has been assigned
  def markersAssigned: Boolean = allFeedback.exists(_.allMarkerFeedback.exists(_.marker != null))

  def isReleasedForMarking: Boolean

  def isReleasedForMarking(usercode: String): Boolean = allFeedback.find(_.usercode == usercode).exists(_.releasedToMarkers)

  def members: UnspecifiedTypeUserGroup
}
