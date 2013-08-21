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
import uk.ac.warwick.tabula.services.RelationshipService

// wrapper class for personal tutor data - just for less crufty method signature
case class PersonalTutorGraph(val tuteeMap: TreeMap[String, Seq[StudentRelationship]], val studentCount: Int, val missingCount: Int)

class ViewPersonalTutorsCommand(val department: Department) extends Command[PersonalTutorGraph] with Unaudited {

	PermissionCheck(Permissions.Profiles.PersonalTutor.Read, mandatory(department))

	var relationshipService = Wire.auto[RelationshipService]

	override def applyInternal(): PersonalTutorGraph = transactional(readOnly = true) {
		// get all tutor/tutee relationships by dept
		val unsortedTutorRelationships = relationshipService.listStudentRelationshipsByDepartment(PersonalTutor, department)

		// group into map by tutor id
		val groupedTutorRelationships = unsortedTutorRelationships.groupBy(_.agent)

		// map id to lastname, where possible, and alpha sort by constructing a TreeMap
		val sortedTutorRelationships = TreeMap((groupedTutorRelationships map {
			case (tutor, tutees) => (StudentRelationship.getLastNameFromAgent(tutor), tutees)
		}).toSeq:_*)

		// count students
		val (studentCount, withTutorsCount) = relationshipService.countStudentsByRelationshipAndDepartment(PersonalTutor, department)

		PersonalTutorGraph(sortedTutorRelationships, studentCount, studentCount - withTutorsCount)
	}
}

class MissingPersonalTutorsCommand(val department: Department) extends Command[(Int, Seq[Member])] with Unaudited {

	PermissionCheck(Permissions.Profiles.PersonalTutor.Read, department)

	var profileService = Wire.auto[ProfileService]
	var relationshipService = Wire.auto[RelationshipService]

	override def applyInternal(): (Int, Seq[Member]) = transactional(readOnly = true) {
		val studentCount = profileService.countStudentsByDepartment(department)
		studentCount match {
			case 0 => (0, Nil)
			case c => (c, relationshipService.listStudentsWithoutRelationship(PersonalTutor, department))
		}
	}
}
