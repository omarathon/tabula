package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService, SortableAgentIdentifier}

import scala.collection.immutable.TreeMap

// wrapper class for relationship data - just for less crufty method signature
case class RelationshipGraph(
	studentMap: TreeMap[SortableAgentIdentifier, Seq[StudentRelationship]],
	missingCount: Int,
	courseMap: Map[StudentCourseDetails, Course],
	yearOfStudyMap: Map[StudentCourseDetails, Int]
)

class ViewStudentRelationshipsCommand(val department: Department, val relationshipType: StudentRelationshipType)
	extends Command[RelationshipGraph] with Unaudited with ReadOnly {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)

	var relationshipService = Wire.auto[RelationshipService]
	var profileService = Wire.auto[ProfileService]


	override def applyInternal(): RelationshipGraph = transactional(readOnly = true) {
		// get all agent/student relationships by dept
		val sortedAgentRelationships = relationshipService.listAgentRelationshipsByDepartment(relationshipType, department)

		val departmentStudentsWithoutAgentCount = relationshipService.listStudentsWithoutRelationship(relationshipType, department).distinct.size

		val courseMap: Map[StudentCourseDetails, Course] = benchmarkTask("courseDetails") {
			relationshipService.coursesForStudentCourseDetails(sortedAgentRelationships.values.flatten.map(_.studentCourseDetails).toSeq)
		}

		val yearOfStudyMap: Map[StudentCourseDetails, Int] = benchmarkTask("yearsOfStudy") {
			relationshipService.latestYearsOfStudyForStudentCourseDetails(sortedAgentRelationships.values.flatten.map(_.studentCourseDetails).toSeq)
		}

		RelationshipGraph(
			sortedAgentRelationships,
			departmentStudentsWithoutAgentCount,
			courseMap,
			yearOfStudyMap
		)
	}
}

class MissingStudentRelationshipCommand(val department: Department, val relationshipType: StudentRelationshipType)
	extends Command[(Int, Seq[Member])] with Unaudited with ReadOnly {

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
