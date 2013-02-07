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
import scala.collection.SortedMap
import scala.collection.immutable.TreeMap
import uk.ac.warwick.tabula.data.model.Member

class ViewPersonalTutorsCommand(val department: Department) extends Command[TreeMap[String, Seq[StudentRelationship]]] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Read, department)

	var profileService = Wire.auto[ProfileService]
	
	override def applyInternal(): TreeMap[String, Seq[StudentRelationship]] = transactional() {
		val unsortedTutorRelationships = profileService.listStudentRelationshipsByDepartment(PersonalTutor, department)
		
		val groupedTutorRelationships = unsortedTutorRelationships.groupBy(_.agentLastName)
		
		TreeMap(groupedTutorRelationships.toSeq:_*)
	}
}

class ViewPersonalTuteesCommand(val currentMember: Member) extends Command[Seq[StudentRelationship]] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.Read, currentMember)

	var profileService = Wire.auto[ProfileService]
	
	override def applyInternal(): Seq[StudentRelationship] = transactional() {
		profileService.listStudentRelationshipsWithMember(PersonalTutor, currentMember)
	}
}
