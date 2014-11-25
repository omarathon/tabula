package uk.ac.warwick.tabula.scheduling.commands

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{Describable, ComposableCommand, CommandInternal, Description}
import uk.ac.warwick.tabula.data.model.FeedbackForSitsStatus.{Failed, Successful}
import uk.ac.warwick.tabula.data.model.{Department, FeedbackForSits}
import uk.ac.warwick.tabula.data.{FeedbackForSitsDaoComponent, AutowiringFeedbackForSitsDaoComponent}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.scheduling.services.{ExportFeedbackToSitsServiceComponent, AutowiringExportFeedbackToSitsServiceComponent}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, FeaturesComponent}

object ExportFeedbackToSitsCommand {
	def apply() = new ExportFeedbackToSitsCommand
		with ComposableCommand[Seq[FeedbackForSits]]
		with ExportFeedbackToSitsCommandPermissions
		with ExportFeedbackToSitsCommandDescription
		with AutowiringFeedbackForSitsDaoComponent
		with AutowiringExportFeedbackToSitsServiceComponent
		with AutowiringFeaturesComponent
}

class ExportFeedbackToSitsCommand extends CommandInternal[Seq[FeedbackForSits]] with Logging {

	self: FeedbackForSitsDaoComponent with ExportFeedbackToSitsServiceComponent with FeaturesComponent with FeedbackForSitsDaoComponent =>

	override def applyInternal() = transactional() {

		val feedbacksToLoad = feedbackForSitsDao.feedbackToLoad
		// for each mark/grade
		for (feedbackToLoad <- feedbacksToLoad) {
			val feedback = feedbackToLoad.feedback
			val feedbackId = feedback.id
			val department = feedback.assignment.module.adminDepartment

			if (!departmentOpen(department, feedback.assignment.academicYear)) {

				val departmentCode = department.code
				logger.warn(f"Not uploading feedback $feedbackId as department $departmentCode is closed")
			}
			else {

				// first check to see if there is one and only one matching blank row
				val rowCount = exportFeedbackToSitsService.countMatchingBlankSasRecords(feedbackToLoad)

				if (rowCount == 0) feedbackToLoad.status = Failed
				else if (rowCount > 1) {
					feedbackToLoad.status = Failed
					logger.warn(f"Not updating SITS CAM_SAS for feedback $feedbackId - found multiple rows")
				}
				else {
					uploadFeedbackToSits(feedbackToLoad)
				}
				feedbackForSitsDao.saveOrUpdate(feedbackToLoad)
			}
		}

		feedbacksToLoad.filter(_.status == Successful)
	}

	def departmentOpen(dept: Department, year: AcademicYear): Boolean = {
		val thisYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		val lastYear = thisYear.previous

		((dept.canUploadMarksToSitsForCurrentYear && (year == thisYear))
			|| (dept.canUploadMarksToSitsForLastYear && (year == lastYear)))
	}

	def uploadFeedbackToSits(feedbackToLoad: FeedbackForSits) {
		val feedback = feedbackToLoad.feedback
		val feedbackId = feedback.id
		val studentId = feedback.universityId

		//  update - expecting to update one row
		val updatedRows = exportFeedbackToSitsService.exportToSits(feedbackToLoad)

		if (updatedRows == 0) feedbackToLoad.status = Failed
		else if (updatedRows == 1) {
			// record what's been done in the FeedbackToLoad object
			feedbackToLoad.status = Successful
			feedbackToLoad.dateOfUpload = DateTime.now
			feedback.actualMark.foreach( mark => feedbackToLoad.actualMarkLastUploaded = mark)
			feedback.actualGrade.foreach( grade => feedbackToLoad.actualGradeLastUploaded = grade)
		}
		else throw new IllegalStateException(f"Unexpected SITS update!  Only expected to update one row, but $updatedRows rows were updated " +
				f"in CAM_SAS for student $studentId, feedback $feedbackId")
	}
}

trait ExportFeedbackToSitsCommandPermissions extends RequiresPermissionsChecking {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.UploadToSits)
	}
}

trait ExportFeedbackToSitsCommandDescription extends Describable[Seq[FeedbackForSits]] {
	override def describe(d: Description) {}
	override def describeResult(d: Description, result: Seq[FeedbackForSits]) {
		d.property("marks/grades written to SITS CAM_SAS", result.size)
	}
}