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
import uk.ac.warwick.courses.SprCode

@Configurable
class ImportAssignmentsCommand extends Command[Unit] with Logging with Daoisms {
	
	@Autowired var assignmentImporter:AssignmentImporter =_
	@Autowired var assignmentService:AssignmentService =_
	
	def apply() { 
		benchmark("ImportAssignments") {
			doAssignments
			logger.debug("Imported UpstreamAssignments. Importing assessment groups...")
			doGroups
			doGroupMembers
		}
	}
	
	@Transactional
	def doAssignments {
		for (assignment <- logSize(assignmentImporter.getAllAssignments)) {
			if (assignment.name == null) {
				// Some SITS data is bad, but try to carry on.
				assignment.name = "Assignment"
			}
			assignmentService.save(assignment)
		}
	}
	
	def doGroups {
		// Split into chunks so we commit transactions periodically.
		for (groups <- logSize(assignmentImporter.getAllAssessmentGroups).grouped(100))
			saveGroups(groups)
	}
	
	/**
	 * This calls the importer method that iterates over ALL module registrations.
	 * The results are ordered such that it can hold a list of items until it
	 * detects one that belongs to a different group, at which point it saves
	 * what it's got and starts a new list. This way we don't have to load many
	 * things into memory at once.
	 */
	@Transactional
	def doGroupMembers {
		benchmark("Import all group members") {
			var registrations = List[ModuleRegistration]()
			var count = 0
			assignmentImporter.allMembers { r =>
				if (!registrations.isEmpty && r.differentGroup(registrations.head)) {
					// This element r is for a new group, so save this group and start afresh
					save(registrations)
					registrations = Nil
				}
				registrations = registrations :+ r 
				count += 1
				if (count % 1000 == 0) {
					logger.info("Processed " + count + " group members")
				}
			}
			logger.info("Processed all " + count + " group members")
		}
	}
	
	/**
	 * This sequence of ModuleRegistrations represents the members of an assessment
	 * group, so save them (and reconcile it with any existing members we have in the
	 * database).
	 */
	def save(group:Seq[ModuleRegistration]) {
		group.headOption map { head =>
			val assessmentGroup = head.toUpstreamAssignmentGroup
			// Convert ModuleRegistrations to simple uni ID strings.
			val members = group map (mr => SprCode.getUniversityId(mr.sprCode))
			assignmentService.replaceMembers(assessmentGroup, members)
		}
	}
	
	@Transactional
	def saveGroups(groups:Seq[UpstreamAssessmentGroup]) = {
		logger.debug("Importing "+groups.size+" assessment groups")
		benchmark("Import "+groups.size+" groups") {
			for (group <- groups) {
				assignmentService.save(group)
				session.evict(group)
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