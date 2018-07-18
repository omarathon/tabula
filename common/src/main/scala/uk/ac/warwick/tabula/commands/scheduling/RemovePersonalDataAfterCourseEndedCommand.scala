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
	val sixYearsAgo: DateTime = DateTime.now().minusYears(6)

	def uniIDsWithEndedCourse(studentCourseDetailsList: Seq[Seq[StudentCourseDetails]]): Seq[String] = {
		studentCourseDetailsList
			.map(_.filter(_.endDate != null))
			.map(_.filter(_.missingFromImportSince != null))
			.map(_.sortWith((l, r) => l.endDate.isAfter(r.endDate))) // course details that ends latest
			.flatMap(_.headOption)
			.filter(_.endDate.isBefore(sixYearsAgo.toLocalDate))
			.filter(_.missingFromImportSince.isBefore(sixYearsAgo))
			.map(_.student.universityId)
	}
}

class RemovePersonalDataAfterCourseEndedCommandInternal
	extends CommandInternal[Seq[String]]
		with Logging
		with RemovePersonalDataAfterCourseEndedCommandHelper {
	self: PermissionsServiceComponent with MemberDaoComponent with StudentCourseDetailsDaoComponent =>
	override protected def applyInternal(): Seq[String] = {
		memberDao.deleteByUniversityIds(
			uniIDsWithEndedCourse(memberDao.getMissingBefore[Member](sixYearsAgo)
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