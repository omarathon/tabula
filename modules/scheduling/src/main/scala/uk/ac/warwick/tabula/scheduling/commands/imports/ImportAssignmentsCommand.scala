package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.{SessionComponent, Daoisms}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.SprCode
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.scheduling.services.AssignmentImporter
import uk.ac.warwick.tabula.scheduling.services.ModuleRegistration
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.{RequiresPermissionsChecking, PermissionsChecking}

object ImportAssignmentsCommand {
	def apply() = new ComposableCommand[Unit]
		with ImportAssignmentsCommand
		with ImportAssignmentsDescription
		with Daoisms

	case class Result(
		assignmentsFound: Int,
		assignmentsChanged: Int,
		groupsFound: Int,
		groupsChanged: Int)
}


trait ImportAssignmentsCommand extends CommandInternal[Unit] with RequiresPermissionsChecking with Logging with SessionComponent {

	def permissionsCheck(p:PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

	var assignmentImporter = Wire.auto[AssignmentImporter]
	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]
	
	val ImportGroupSize = 100

	def applyInternal() {
		benchmark("ImportAssessment") {
			doAssignments()
			logger.debug("Imported AssessmentComponents. Importing assessment groups...")
			doGroups()
			doGroupMembers()
		}
	}

	def doAssignments() {
		transactional() {
			for (assignment <- logSize(assignmentImporter.getAllAssessmentComponents)) {
				if (assignment.name == null) {
					// Some SITS data is bad, but try to carry on.
					assignment.name = "Assessment Component"
				}
				assignmentMembershipService.save(assignment)
			}
		}
	}

	def doGroups() {
		// Split into chunks so we commit transactions periodically.
		for (groups <- logSize(assignmentImporter.getAllAssessmentGroups).grouped(ImportGroupSize)) {
			saveGroups(groups)
			transactional() {
				groups foreach session.evict
			}
		}
	}

	/**
	 * This calls the importer method that iterates over ALL module registrations.
	 * The results are ordered such that it can hold a list of items until it
	 * detects one that belongs to a different group, at which point it saves
	 * what it's got and starts a new list. This way we don't have to load many
	 * things into memory at once.
	 */
	def doGroupMembers() {
		transactional() {
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
				// TAB-1265 Don't forget the very last bunch.
				if (!registrations.isEmpty) {
					save(registrations)
				}
				logger.info("Processed all " + count + " group members")
			}
		}
	}

	/**
	 * This sequence of ModuleRegistrations represents the members of an assessment
	 * group, so save them (and reconcile it with any existing members we have in the
	 * database).
	 */
	def save(group: Seq[ModuleRegistration]) {
		group.headOption map { head =>
			val assessmentGroup = head.toUpstreamAssignmentGroup
			// Convert ModuleRegistrations to simple uni ID strings.
			val members = group map (mr => SprCode.getUniversityId(mr.sprCode))
			assignmentMembershipService.replaceMembers(assessmentGroup, members)
		}
	}

	
	def saveGroups(groups: Seq[UpstreamAssessmentGroup]) = transactional() {
		logger.debug("Importing " + groups.size + " assessment groups")
		benchmark("Import " + groups.size + " groups") {
			for (group <- groups) {
				assignmentMembershipService.save(group)
			}
		}
	}

}


trait ImportAssignmentsDescription extends Describable[Unit] {
	def describe(d: Description) {}
}