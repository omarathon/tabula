package uk.ac.warwick.tabula.scheduling.commands.imports

import org.hibernate.Session
import org.scalatest.junit._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith

import uk.ac.warwick.tabula.{AcademicYear, CustomHamcrestMatchers, Mockito}
import uk.ac.warwick.tabula.scheduling.services.AssignmentImporter
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, AssessmentMembershipService}
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.scheduling.services.UpstreamModuleRegistration

@RunWith(classOf[JUnitRunner])
class ImportAssignmentsCommandTest extends FlatSpec with Matchers with Mockito {

	trait Fixture {
		val mockSession = smartMock[Session]
		val importer = smartMock[AssignmentImporter]
		val membershipService = smartMock[AssessmentMembershipService]
		val moduleService = smartMock[ModuleAndDepartmentService]
		val command = new ImportAssignmentsCommand {
			def session = mockSession
		}
		command.assignmentImporter = importer
		command.assignmentMembershipService = membershipService
		command.moduleAndDepartmentService = moduleService

		moduleService.getModuleByCode(any[String]) returns (None) // Not necessary for this to work
		membershipService.replaceMembers(any[UpstreamAssessmentGroup], any[Seq[String]]) answers { args =>
			val uag = args.asInstanceOf[Array[_]](0).asInstanceOf[UpstreamAssessmentGroup]
			uag.id = "seenGroupId"

			uag
		}

		val registrations: Seq[UpstreamModuleRegistration]

		importer.allMembers(any[UpstreamModuleRegistration=>Unit]) answers { _ match {
			case fn: (UpstreamModuleRegistration=>Unit) @unchecked => registrations.foreach(fn)
		}}
	}

	behavior of "doGroupMembers"

	it should "process all collections" in {
		new Fixture {
			membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns (Nil)

			val registrations = Seq(
				UpstreamModuleRegistration("13/14", "0100001/1", "A", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100001/1", "A", "HI100-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "A", "HI101-30", "A")
			)
			command.doGroupMembers()
			there were three(membershipService).replaceMembers(any[UpstreamAssessmentGroup], any[Seq[String]])
		}
	}

	/**
	 * TAB-1265
	 */
	it should "process empty groups" in {
		new Fixture {
			val hi900_30 = {
				val g = new UpstreamAssessmentGroup
				g.moduleCode = "HI900-30"
				g.occurrence = "A"
				g.assessmentGroup = "A"
				g.academicYear = AcademicYear.parse("13/14")
				g
			}

			val registrations = Seq(
				UpstreamModuleRegistration("13/14", "0100001/1", "A", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "A", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100003/1", "A", "HI100-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "A", "HI100-30", "A")
			)

			membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns (Seq(hi900_30))

			command.doGroupMembers()

			there was one(membershipService).replaceMembers(anArgThat(hasModuleCode("HI33M-30")), isEq(Seq("0100001", "0100002")))
			there was one(membershipService).replaceMembers(anArgThat(hasModuleCode("HI100-30")), isEq(Seq("0100003", "0100002")))

			// The bug is that we don't update any group we don't have moduleregistrations for.
			there was one(membershipService).replaceMembers(anArgThat(hasModuleCode("HI900-30")), isEq(Nil))

		}
	}

	/** Matches on an UpstreamAssessmentGroup's module code. */
	def hasModuleCode(code: String) = CustomHamcrestMatchers.hasProperty('moduleCode, code)


}
