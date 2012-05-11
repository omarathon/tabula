package uk.ac.warwick.courses.commands.imports

import uk.ac.warwick.courses.services._
import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional


@Configurable
class ImportAssignmentsCommand extends Command[Unit] with Logging with Daoisms {
	
	@Autowired var assignmentImporter:AssignmentImporter =_
	@Autowired var assignmentService:AssignmentService =_
	
	
	
	def apply() { 
		benchmark("ImportAssignments") {
			doAssignments
			doGroups
		}
	}
	
	@Transactional
	def doAssignments {
		for (assignment <- logSize(assignmentImporter.getAllAssignments))
			assignmentService.save(assignment)
	}
	
	def doGroups {
		// Split into chunks so we commit transactions periodically.
		for (groups <- logSize(assignmentImporter.getAllAssessmentGroups).grouped(500))
			saveGroups(groups)
	}
	
	@Transactional
	def saveGroups(groups:Seq[UpstreamAssessmentGroup]) = {
		for (group <- groups) {
			assignmentService.save(group)
			
			//assignmentImporter.getMembersOf(group)
		}
	}

	def describe(d: Description) {
		
	}

	
}

object ImportAssignmentsCommand {
	case class Result(
			val assignmentsFound:Int, 
			val assignmentsChanged:Int, 
			val groupsFound:Int, 
			val groupsChanged:Int)
}