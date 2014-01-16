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

class ImportTier4ForStudentCommand(student: StudentMember)
	extends Command[Boolean] with Unaudited with Logging {
	PermissionCheck(Permissions.ImportSystemData)

	var requirementImporter = Wire[Tier4RequirementImporter]
	var memberDao = Wire[MemberDao]

	def applyInternal(): Boolean = {
		val newRequirement = requirementImporter.hasTier4Requirement(student.universityId)
		if (student.tier4VisaRequirement != newRequirement) {
			student.tier4VisaRequirement = newRequirement
			memberDao.saveOrUpdate(student)
			true
		}
		else false
	}
}
