package uk.ac.warwick.tabula.scheduling.commands.imports
import uk.ac.warwick.spring.Wire

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.data.model.RelationshipType.Supervisor
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.services.SupervisorImporter
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.RelationshipService

class ImportSupervisorsForSingleStudentCommand(studentCourseDetails: StudentCourseDetails) extends Command[Unit] with Logging{
	PermissionCheck(Permissions.ImportSystemData)

	var supervisorImporter = Wire.auto[SupervisorImporter]
	var profileService = Wire.auto[ProfileService]
	var relationshipService = Wire.auto[RelationshipService]

	def applyInternal() {
		if (studentCourseDetails.route != null && studentCourseDetails.route.degreeType == Postgraduate) {
			transactional() {
				importSupervisors
			}
		}
	}

	override def describe(d: Description) = d.property("sprCode" -> studentCourseDetails.sprCode)

	def importSupervisors {
		val prsCodes = supervisorImporter.getSupervisorPrsCodes(studentCourseDetails.scjCode)

		supervisorImporter.getSupervisorPrsCodes(studentCourseDetails.scjCode).foreach {
			supervisorPrsCode => {
				profileService.getMemberByPrsCode(supervisorPrsCode) match {
					case Some(sup: StaffMember) => relationshipService.saveStudentRelationship(Supervisor, studentCourseDetails.sprCode, sup.id)
					case _ => logger.warn("Can't save supervisor " + supervisorPrsCode + " for " + studentCourseDetails.sprCode + " - not a member in Tabula db")
				}
			}
		}
	}
}
