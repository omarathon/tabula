package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.PersonalTutor

class ViewPersonalTutorsCommand(val department: Department) extends Command[Seq[StudentRelationship]] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Read, department)

	var profileService = Wire.auto[ProfileService]
	
	override def applyInternal(): Seq[StudentRelationship] = transactional() {
		profileService.listStudentRelationshipsByDepartment(PersonalTutor, department)
	}
}