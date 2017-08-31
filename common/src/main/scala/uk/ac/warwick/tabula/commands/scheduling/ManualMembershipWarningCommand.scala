package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, Notification}
import uk.ac.warwick.tabula.data.model.notifications.ManualMembershipWarningNotification
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.services._


object ManualMembershipWarningCommand {
	def apply() = new ManualMembershipWarningCommandInternal()
		with ComposableCommand[Seq[Department]]
		with PubliclyVisiblePermissions with ReadOnly with Unaudited
		with AutowiringAssessmentMembershipServiceComponent
		with ManualMembershipWarningNotifications
}

class ManualMembershipWarningCommandInternal() extends CommandInternal[Seq[Department]] with Logging {

	self: AssessmentMembershipServiceComponent =>

	def applyInternal(): Seq[Department] = {
		benchmark("ManualMembershipWarning") {
			val currentSITSAcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())
			assessmentMembershipService.departmentsWithManualAssessmentsOrGroups(currentSITSAcademicYear)
		}
	}
}

trait ManualMembershipWarningNotifications extends Notifies[Seq[Department], Department] {
	def emit(departments: Seq[Department]): Seq[ManualMembershipWarningNotification] =
		departments.map(Notification.init(new ManualMembershipWarningNotification, null, _))
}

