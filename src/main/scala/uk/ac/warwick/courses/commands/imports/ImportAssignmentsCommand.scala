package uk.ac.warwick.courses.commands.imports

import uk.ac.warwick.courses.services._
import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional
import collection.JavaConversions._

@Configurable
class ImportAssignmentsCommand extends Command[Unit] with Logging with Daoisms {
	
	@Autowired var assignmentImporter:AssignmentImporter =_
	@Autowired var assignmentService:AssignmentService =_
	
	def apply() { 
		benchmark("ImportAssignments") {
			doAssignments
			logger.debug("Imported UpstreamAssignments. Importing assessment groups...")
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
		for (groups <- logSize(assignmentImporter.getAllAssessmentGroups).grouped(100))
			saveGroups(groups)
	}
	
	@Transactional
	def saveGroups(groups:Seq[UpstreamAssessmentGroup]) = {
		logger.debug("Importing "+groups.size+" assessment groups and their members")
		benchmark("Import "+groups.size+" groups") {
			for (group <- groups) {
				val members = assignmentImporter.getMembers(group)
				if (!equals(members, group.members.staticIncludeUsers)) {
					group.members.staticIncludeUsers.clear
					group.members.staticIncludeUsers.addAll(members)
				}
				assignmentService.save(group)
			}
		}
	}
	
	def equal(s1:Seq[String], s2:Seq[String]) = 
		s1.length == s2.length && s1.sorted == s2.sorted

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