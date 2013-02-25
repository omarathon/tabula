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
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import scala.collection.SortedMap
import scala.collection.immutable.TreeMap
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType.Student

class ViewPersonalTutorsCommand(val department: Department) extends Command[TreeMap[String, Seq[StudentRelationship]]] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Read, department)

	var profileService = Wire.auto[ProfileService]
	
	override def applyInternal(): TreeMap[String, Seq[StudentRelationship]] = transactional() {
		val unsortedTutorRelationships = profileService.listStudentRelationshipsByDepartment(PersonalTutor, department)
		
		val groupedTutorRelationships = unsortedTutorRelationships.groupBy(_.agentLastName)
		
		TreeMap(groupedTutorRelationships.toSeq:_*)
	}
}

class MissingPersonalTutorsCommand(val department: Department) extends Command[(Int, Seq[Member])] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Read, department)

	var profileService = Wire.auto[ProfileService]
	
	override def applyInternal(): (Int, Seq[Member]) = transactional() {
		val ownStudentCount = profileService.findMembersByDepartment(department, false, Set(Student)).size
		ownStudentCount match {
			case 0 => (0, Nil)
			case c => (c, profileService.listStudentsWithoutRelationship(PersonalTutor, department))
		}
	}
}

class ViewPersonalTuteesCommand(val currentMember: Member) extends Command[Seq[StudentRelationship]] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.Read, currentMember)

	var profileService = Wire.auto[ProfileService]
	
	override def applyInternal(): Seq[StudentRelationship] = transactional() {
		profileService.listStudentRelationshipsWithMember(PersonalTutor, currentMember)
	}
}
