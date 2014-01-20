package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.services.Tier4RequirementImporter
import uk.ac.warwick.tabula.scheduling.services.CasUsageImporter
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao
import uk.ac.warwick.tabula.AcademicYear

class ImportCasUsageForStudentCommand(student: StudentMember, year: AcademicYear)
	extends Command[Boolean] with Unaudited with Logging {
	PermissionCheck(Permissions.ImportSystemData)

	var casUsageImporter = Wire[CasUsageImporter]
	var scydDao = Wire[StudentCourseYearDetailsDao]

	def applyInternal(): Boolean = {
		var found = false
		val newCasUsed = casUsageImporter.isCasUsed(student.universityId)
		student.freshOrStaleStudentCourseYearDetails(year).map {
			scyd => {
				if (scyd.casUsed != newCasUsed) {
					scyd.casUsed = newCasUsed
					scydDao.saveOrUpdate(scyd)
					found = true
				}
			}
		}
		found
	}
}
