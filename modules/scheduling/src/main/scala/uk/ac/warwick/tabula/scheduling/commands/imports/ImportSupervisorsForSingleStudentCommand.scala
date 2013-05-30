package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.services.SupervisorImporter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.RelationshipType.Supervisor
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate

class ImportSupervisorsForSingleStudentCommand(member: StudentMember) extends Command[Unit] with Daoisms {
	PermissionCheck(Permissions.ImportSystemData)

	val supervisorImporter = Wire.auto[SupervisorImporter]
	val profileService = Wire.auto[ProfileService]

	def applyInternal() {
		if (member.studyDetails.route.degreeType == Postgraduate) {
			transactional() {
				importSupervisors
			}
		}
	}

	override def describe(d: Description) = d.property("universityId" -> member.universityId)

	def importSupervisors {
		val prsCodes = supervisorImporter.getSupervisorPrsCodes(member.studyDetails.scjCode)
		for (
				supervisorPrsCode <- supervisorImporter.getSupervisorPrsCodes(member.studyDetails.scjCode);
				supervisor <- profileService.getMemberByPrsCode(supervisorPrsCode)
		) {
			val supervisorId = supervisor.id
			if (!profileService.getRelationships(Supervisor, member.studyDetails.sprCode).exists {
				_.agent == supervisorId
			}) {
				profileService.saveStudentRelationship(Supervisor, member.studyDetails.sprCode, supervisorId)
			}
		}
	}
}
