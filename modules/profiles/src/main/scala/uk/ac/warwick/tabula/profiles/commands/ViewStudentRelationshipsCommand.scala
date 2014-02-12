package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.immutable.TreeMap
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

// wrapper class for relationship data - just for less crufty method signature
case class RelationshipGraph(studentMap: TreeMap[SortableAgentIdentifier, Seq[StudentRelationship]], studentCount: Int, missingCount: Int)
case class SortableAgentIdentifier(agentId:String, lastName:Option[String]){
	 val sortkey = lastName.getOrElse("") + agentId
	 override def toString() = sortkey
}
object SortableAgentIdentifier{
	def apply(r:StudentRelationship) = new SortableAgentIdentifier(r.agent, r.agentMember.map(_.lastName))

	val KeyOrdering = Ordering.by { a:SortableAgentIdentifier => a.sortkey }
}

class ViewStudentRelationshipsCommand(val department: Department, val relationshipType: StudentRelationshipType) extends Command[RelationshipGraph] with Unaudited {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)

	var relationshipService = Wire.auto[RelationshipService]
	var profileService = Wire.auto[ProfileService]


	override def applyInternal(): RelationshipGraph = transactional(readOnly = true) {
		// get all agent/student relationships by dept

		// all students in department X
		val unsortedAgentRelationshipsByStudentDept = relationshipService.listStudentRelationshipsByDepartment(relationshipType, department)

		// all students with a tutor in department X
		val unsortedAgentRelationshipsByStaffDept = relationshipService.listStudentRelationshipsByStaffDepartment(relationshipType, department)

		// combine the two and remove the dups
		val unsortedAgentRelationships = (unsortedAgentRelationshipsByStudentDept ++unsortedAgentRelationshipsByStaffDept).distinct

		// group into map by agent lastname, or id if the lastname is unavailable
		val groupedAgentRelationships = unsortedAgentRelationships.groupBy(r=>SortableAgentIdentifier(r))

		//  alpha sort by constructing a TreeMap
		val sortedAgentRelationships = TreeMap((groupedAgentRelationships).toSeq:_*)(SortableAgentIdentifier.KeyOrdering)

		// count students
		val studentsInDepartmentCount = profileService.countStudentsByDepartment(department)
		val studentsWithAgentsCount = unsortedAgentRelationships.map(_.studentId).distinct.size

		val studentIdsInDepartment = unsortedAgentRelationshipsByStudentDept.map(_.studentId).distinct
		val studentsOutsideDepartmentCount =
			unsortedAgentRelationshipsByStaffDept.map(_.studentId).distinct
				.filterNot(id=>studentIdsInDepartment.exists(_ == id)).size

		RelationshipGraph(sortedAgentRelationships, studentsInDepartmentCount + studentsOutsideDepartmentCount, (studentsInDepartmentCount + studentsOutsideDepartmentCount) - studentsWithAgentsCount)
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
