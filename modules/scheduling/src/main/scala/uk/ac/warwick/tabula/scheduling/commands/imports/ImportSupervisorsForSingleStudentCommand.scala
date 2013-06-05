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
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.StaffMember

class ImportSupervisorsForSingleStudentCommand(member: StudentMember) extends Command[Unit] with Logging{
	PermissionCheck(Permissions.ImportSystemData)

	var supervisorImporter = Wire.auto[SupervisorImporter]
	var profileService = Wire.auto[ProfileService]

	def applyInternal() {
		logger.info("member is: " + member)
		if (member.studyDetails.route != null && member.studyDetails.route.degreeType == Postgraduate) {
			transactional() {
				importSupervisors
			}
		}
	}

	override def describe(d: Description) = d.property("universityId" -> member.universityId)

	def importSupervisors {
		val prsCodes = supervisorImporter.getSupervisorPrsCodes(member.studyDetails.scjCode)

		supervisorImporter.getSupervisorPrsCodes(member.studyDetails.scjCode).foreach {
			supervisorPrsCode => {
				profileService.getMemberByPrsCode(supervisorPrsCode) match {
					case Some(sup: StaffMember) => profileService.saveStudentRelationship(Supervisor, member.studyDetails.sprCode, sup.id)
					case _ => logger.warn("Can't save supervisor " + supervisorPrsCode + " for " + member.studyDetails.sprCode + " - not a member in Tabula db")
				}
			}
		}
	}
}
