package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.{AutowiringAssignmentMembershipServiceComponent, AssignmentMembershipServiceComponent}
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._

class UpstreamAssessmentGroupCreationFixtureCommandInternal extends CommandInternal[UpstreamAssessmentGroup] {
	 self: AssignmentMembershipServiceComponent with TransactionalComponent =>

	 var moduleCode: String = _
	 var assessmentGroup = "A"
	 var occurrence = "A"
	 var universityIds: JList[String] = JArrayList()

	 def applyInternal() = transactional() {
		 val group = new UpstreamAssessmentGroup
		 group.moduleCode = moduleCode
		 group.occurrence = occurrence
		 group.assessmentGroup = assessmentGroup
		 group.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		 group.members.knownType.staticUserIds = universityIds.asScala

		 assignmentMembershipService.save(group)
		 group
	 }

 }

object UpstreamAssessmentGroupCreationFixtureCommand {
	def apply() =
		new UpstreamAssessmentGroupCreationFixtureCommandInternal
			with ComposableCommand[UpstreamAssessmentGroup]
			with AutowiringAssignmentMembershipServiceComponent
			with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions
}

