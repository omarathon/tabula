package uk.ac.warwick.tabula.scheduling.helpers

import uk.ac.warwick.tabula.scheduling.commands.imports.{ImportStudentCourseYearCommand, ImportStudentCourseCommand, ImportSupervisorsForStudentCommand}
import uk.ac.warwick.tabula.scheduling.services.{AwardImporter, CourseImporter, SitsStatusImporter, ModeOfAttendanceImporter, SupervisorImporter}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentCourseDetails}
import uk.ac.warwick.tabula.services.{ProfileService, CourseAndRouteService, ModuleAndDepartmentService, RelationshipService, MaintenanceModeService}
import uk.ac.warwick.tabula.data.{StudentCourseDetailsDao, StudentCourseYearDetailsDao, ModeOfAttendanceDao, MemberDao}

/**
 * Created by zoe on 13/02/14.
 */

class ImportCommandFactory() {
	val rowTracker = new ImportRowTracker
	var test = false

	// This lot are all normally auto-wired within the import commands.  Declared here in order to allow us to
	// overwrite them with mock versions for testing.

	// needed for ImportStudentCourseCommand:
	var memberDao: MemberDao = _
	var relationshipService: RelationshipService = _
	var studentCourseDetailsDao: StudentCourseDetailsDao = _
	var courseAndRouteService: CourseAndRouteService = _
	var courseImporter: CourseImporter = _
	var awardImporter: AwardImporter = _

	// needed for PropertyCopying, extended by ImportStudentCourseCommand:
	var sitsStatusImporter: SitsStatusImporter = _
	var modAndDeptService: ModuleAndDepartmentService = _

	// needed for ImportStudentCourseYearCommand:
	var moaDao: ModeOfAttendanceDao = _
	var profileService: ProfileService = _
	var studentCourseYearDetailsDao: StudentCourseYearDetailsDao = _
	var modeOfAttendanceImporter: ModeOfAttendanceImporter = _

	// needed by Command
	var maintenanceModeService: MaintenanceModeService = _

	def createImportStudentCourseCommand(row: SitsStudentRow, stuMem: StudentMember) = {
		val command = new ImportStudentCourseCommand(row, stuMem, this)

		if (test) {
			// needed directly by ImportStudentCourseCommand
			command.memberDao = memberDao
			command.relationshipService = relationshipService
			command.studentCourseDetailsDao = studentCourseDetailsDao
			command.courseAndRouteService = courseAndRouteService
			command.courseImporter = courseImporter
			command.awardImporter = awardImporter

			// needed by PropertyCopying, extended by ImportStudentCourseCommand
			command.sitsStatusImporter = sitsStatusImporter
			command.moduleAndDepartmentService = modAndDeptService

			// needed by Command, extended by ImportStudentCourseCommand
			command.maintenanceMode = maintenanceModeService
		}
		command
	}

	def createImportStudentCourseYearCommand(row: SitsStudentRow, studentCourseDetails: StudentCourseDetails) = {
		val command = new ImportStudentCourseYearCommand(row, studentCourseDetails, rowTracker)
		if (test) {
			command.modeOfAttendanceImporter = modeOfAttendanceImporter
			command.profileService = profileService
			command.sitsStatusImporter = sitsStatusImporter
			command.maintenanceMode = maintenanceModeService
			command.studentCourseYearDetailsDao = studentCourseYearDetailsDao
		}
		command
	}
}
