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

// wrapper class for personal tutor data - just for less crufty method signature
class PersonalTutorGraph(val tuteeMap: TreeMap[String, Seq[StudentRelationship]], val studentCount: Int, val missingCount: Int)

class ViewPersonalTutorsCommand(val department: Department) extends Command[PersonalTutorGraph] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Read, department)

	var profileService = Wire[ProfileService]
	
	override def applyInternal(): PersonalTutorGraph = transactional(readOnly = true) {
		// get all tutor/tutee relationships by dept
		val unsortedTutorRelationships = profileService.listStudentRelationshipsByDepartment(PersonalTutor, department)
		
		// group into map by tutor id
		val groupedTutorRelationships = unsortedTutorRelationships.groupBy(_.agent)
		
		// map id to lastname, where possible, and alpha sort by constructing a TreeMap
		val sortedTutorRelationships = TreeMap((groupedTutorRelationships map {
			case (tutor, tutees) => (StudentRelationship.getLastNameFromAgent(tutor), tutees)
		}).toSeq:_*)
		
		// count students
		val (studentCount, missingCount) = profileService.countStudentsByRelationshipAndDepartment(PersonalTutor, department)
		
		new PersonalTutorGraph(sortedTutorRelationships, studentCount, missingCount)
	}
}

class MissingPersonalTutorsCommand(val department: Department) extends Command[(Int, Seq[Member])] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Read, department)

	var profileService = Wire[ProfileService]
	
	override def applyInternal(): (Int, Seq[Member]) = transactional(readOnly = true) {
		val studentCount = profileService.countStudentsByDepartment(department)
		studentCount match {
			case 0 => (0, Nil)
			case c => (c, profileService.listStudentsWithoutRelationship(PersonalTutor, department))
		}
	}
}

class ViewPersonalTuteesCommand(val currentMember: Member) extends Command[Seq[StudentRelationship]] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.Read.PersonalTutees, currentMember)

	var profileService = Wire[ProfileService]
	
	override def applyInternal(): Seq[StudentRelationship] = transactional(readOnly = true) {
		profileService.listStudentRelationshipsWithMember(PersonalTutor, currentMember)
	}
}
