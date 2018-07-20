package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.{AutowiringMemberDaoComponent, AutowiringStudentCourseDetailsDaoComponent, MemberDaoComponent, StudentCourseDetailsDaoComponent}
import uk.ac.warwick.tabula.data.model.{Member, StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RemovePersonalDataAfterCourseEndedCommand {
	def apply() =
		new RemovePersonalDataAfterCourseEndedCommandInternal
			with ComposableCommand[Seq[String]]
			with AutowiringMemberDaoComponent
			with AutowiringStudentCourseDetailsDaoComponent
			with AutowiringPermissionsServiceComponent
			with RemovePersonalDataAfterCourseEndedCommandPermission
			with RemovePersonalDataAfterCourseEndedCommandDescription
}

trait RemovePersonalDataAfterCourseEndedCommandHelper {
	def uniIDsWithEndedCourse(studentCourseDetailsList: Seq[Seq[StudentCourseDetails]]): Seq[String] = {
		studentCourseDetailsList
			.filter(_.nonEmpty)
			.map(details => (details.head.student.universityId, details))
			.map {
				case (uniId, detailsList) =>
					(uniId, detailsList.filter(_.endDate != null).filter(_.missingFromImportSince != null))
			}
			.filter {
				case (_, detailsList) => detailsList.nonEmpty
			}
			.map {
				case (uniId, detailsList) =>
					val latestCourseDetails = detailsList
						.sortWith((l, r) => l.endDate.isAfter(r.endDate))
						.head
					(uniId, latestCourseDetails)
			}
			.flatMap {
				case (uniId, details) =>
					val ended = details.endDate.isBefore(DateTime.now().minusYears(6).toLocalDate)
					val missing = details.missingFromImportSince.isBefore(DateTime.now().minusYears(1))
					// the student we want to remove if course ended > 6 years ago
					// and also student course details missing from SITS > 1 year ago
					if (ended && missing) Some(uniId) else None
			}
	}
}

class RemovePersonalDataAfterCourseEndedCommandInternal
	extends CommandInternal[Seq[String]]
		with Logging
		with RemovePersonalDataAfterCourseEndedCommandHelper {
	self: PermissionsServiceComponent with MemberDaoComponent with StudentCourseDetailsDaoComponent =>
	override protected def applyInternal(): Seq[String] = {
		memberDao.deleteByUniversityIds(
			// target students who's been missing from import for a year
			uniIDsWithEndedCourse(memberDao.getMissingBefore[StudentMember](DateTime.now().minusYears(1))
				.map(studentCourseDetailsDao.getByUniversityId)
			)
		)
	}
}


trait RemovePersonalDataAfterCourseEndedCommandPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait RemovePersonalDataAfterCourseEndedCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.studentIds(result)
	}
}