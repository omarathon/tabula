package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.services.AssignmentImporter
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

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

	var assignmentImporter = Wire[AssignmentImporter]
	var assignmentMembershipService = Wire[AssessmentMembershipService]
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
	
	val ImportGroupSize = 100

	def applyInternal() {
		benchmark("ImportAssessment") {
			doAssignments()
			logger.debug("Imported AssessmentComponents. Importing assessment groups...")
			doGroups()
			doGroupMembers()
			logger.debug("Imported assessment groups. Importing grade boundaries...")
			doGradeBoundaries()
		}
	}

	def doAssignments() {
		val assessmentComponents = logSize(assignmentImporter.getAllAssessmentComponents)
		val modules = moduleAndDepartmentService.getModulesByCodes(assessmentComponents.map(_.moduleCode).distinct)
			.groupBy(_.code).mapValues(_.head)
		for (assignments <- assessmentComponents.grouped(ImportGroupSize)) {
			transactional() {
				for (assignment <- assignments) {
					if (assignment.name == null) {
						// Some SITS data is bad, but try to carry on.
						assignment.name = "Assessment Component"
					}

					modules.get(assignment.moduleCodeBasic.toLowerCase).foreach(module => assignment.module = module)
					assignmentMembershipService.save(assignment)
				}
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
				var registrations = List[UpstreamModuleRegistration]()
				var notEmptyGroupIds = Set[String]()

				var count = 0
				assignmentImporter.allMembers { r =>
					if (registrations.nonEmpty && r.differentGroup(registrations.head)) {
						// This element r is for a new group, so save this group and start afresh
						save(registrations)
							.foreach { uag => notEmptyGroupIds = notEmptyGroupIds + uag.id }

						registrations = Nil
					}
					registrations = registrations :+ r
					count += 1
					if (count % 1000 == 0) {
						logger.info("Processed " + count + " group members")
					}
				}

				// TAB-1265 Don't forget the very last bunch.
				if (registrations.nonEmpty) {
					save(registrations)
						.foreach { uag => notEmptyGroupIds = notEmptyGroupIds + uag.id }
				}

				// Empty groups that we haven't seen for academic years
				assignmentMembershipService.getUpstreamAssessmentGroupsNotIn(
					ids = notEmptyGroupIds.filter { _.hasText }.toSeq,
					academicYears = assignmentImporter.yearsToImport
				).foreach { emptyGroup =>
					assignmentMembershipService.replaceMembers(emptyGroup, Nil)
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
	def save(group: Seq[UpstreamModuleRegistration]): Option[UpstreamAssessmentGroup] = {
		group.headOption.map { head =>
			val assessmentGroup = head.toUpstreamAssignmentGroup
			// TAB-3388 change seat number to null if there is ambiguity
			val fixedGroup = group.groupBy(m => (m.year, m.sprCode, m.occurrence, m.moduleCode, m.assessmentGroup)).mapValues(regs =>
				if (regs.size > 1) {
					val r = regs.head
					UpstreamModuleRegistration(r.year, r.sprCode, null, r.occurrence, r.moduleCode, r.assessmentGroup)
				} else {
					regs.head
				}
			).values.toSeq
			assignmentMembershipService.replaceMembers(assessmentGroup, fixedGroup)
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

	def doGradeBoundaries() {
		transactional() {
			val boundaries = assignmentImporter.getAllGradeBoundaries
			boundaries.groupBy(_.marksCode).keys.foreach(assignmentMembershipService.deleteGradeBoundaries)
			for (gradeBoundary <- logSize(assignmentImporter.getAllGradeBoundaries)) {
				assignmentMembershipService.save(gradeBoundary)
			}
		}
	}

}


trait ImportAssignmentsDescription extends Describable[Unit] {
	def describe(d: Description) {}
}