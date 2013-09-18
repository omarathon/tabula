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
import scala.collection.SortedMap
import scala.collection.immutable.TreeMap
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

// wrapper class for relationship data - just for less crufty method signature
case class RelationshipGraph(val studentMap: TreeMap[String, Seq[StudentRelationship]], val studentCount: Int, val missingCount: Int)

class ViewStudentRelationshipsCommand(val department: Department, val relationshipType: StudentRelationshipType) extends Command[RelationshipGraph] with Unaudited {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)

	var relationshipService = Wire.auto[RelationshipService]

	override def applyInternal(): RelationshipGraph = transactional(readOnly = true) {
		// get all agent/student relationships by dept
		val unsortedAgentRelationships = relationshipService.listStudentRelationshipsByDepartment(relationshipType, department)

		// group into map by agent id
		val groupedAgentRelationships = unsortedAgentRelationships.groupBy(_.agent)

		// map id to lastname, where possible, and alpha sort by constructing a TreeMap
		val sortedAgentRelationships = TreeMap((groupedAgentRelationships map {
			case (agent, students) => (StudentRelationship.getLastNameFromAgent(agent), students)
		}).toSeq:_*)

		// count students
		val (studentCount, withAgentsCount) = relationshipService.countStudentsByRelationshipAndDepartment(relationshipType, department)

		RelationshipGraph(sortedAgentRelationships, studentCount, studentCount - withAgentsCount)
	}
}

class MissingStudentRelationshipCommand(val department: Department, val relationshipType: StudentRelationshipType) extends Command[(Int, Seq[Member])] with Unaudited {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)

	var profileService = Wire.auto[ProfileService]
	var relationshipService = Wire.auto[RelationshipService]

	override def applyInternal(): (Int, Seq[Member]) = transactional(readOnly = true) {
		val studentCount = profileService.countStudentsByDepartment(department)
		studentCount match {
			case 0 => (0, Nil)
			case c => (c, relationshipService.listStudentsWithoutRelationship(relationshipType, department))
		}
	}
}
