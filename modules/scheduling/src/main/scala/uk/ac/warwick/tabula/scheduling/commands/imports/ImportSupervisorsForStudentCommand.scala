package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.spring.Wire

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.services.SupervisorImporter
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.RelationshipService
import org.joda.time.DateTime

class ImportSupervisorsForStudentCommand()
	extends Command[Unit] with Unaudited with Logging {
	PermissionCheck(Permissions.ImportSystemData)

	var supervisorImporter = Wire.auto[SupervisorImporter]
	var profileService = Wire.auto[ProfileService]
	var relationshipService = Wire.auto[RelationshipService]

	var studentCourseDetails: StudentCourseDetails = _

	def applyInternal() {
		if (studentCourseDetails.route != null && studentCourseDetails.route.degreeType == Postgraduate) {
			transactional() {
				importSupervisors()
			}
		}
	}

	override def describe(d: Description) = d.property("sprCode" -> studentCourseDetails.sprCode)

	def importSupervisors() {
		relationshipService
			.getStudentRelationshipTypeByUrlPart("supervisor") // TODO this is awful
			.filter { relType =>
				val source = Option(studentCourseDetails.department).map { _.getStudentRelationshipSource(relType) }.getOrElse(StudentRelationshipSource.SITS)
				source == StudentRelationshipSource.SITS
			}
			.foreach { relationshipType =>
				val supervisorPrsCodes = supervisorImporter.getSupervisorPrsCodes(studentCourseDetails.scjCode)
				val supervisors = supervisorPrsCodes.flatMap { prsCode =>
					val m = profileService.getMemberByPrsCode(prsCode)
					if (m.isEmpty) {
						logger.warn("Can't save supervisor " + prsCode + " for " + studentCourseDetails.sprCode + " - not a member in Tabula db")
					}
					m
				}
				
				relationshipService.replaceStudentRelationship(relationshipType, studentCourseDetails.sprCode, supervisors.map { _.universityId })
			}
	}
}
