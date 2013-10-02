package uk.ac.warwick.tabula.scheduling.commands.imports

import org.hibernate.Session
import org.scalatest.junit._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith

import uk.ac.warwick.tabula.{CustomHamcrestMatchers, Mockito}
import uk.ac.warwick.tabula.scheduling.services.AssignmentImporter
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.scheduling.services.ModuleRegistration

@RunWith(classOf[JUnitRunner])
class ImportAssignmentsCommandTest extends FlatSpec with ShouldMatchers with Mockito {

	trait Fixture {
		val mockSession = smartMock[Session]
		val importer = smartMock[AssignmentImporter]
		val membershipService = smartMock[AssignmentMembershipService]
		val command = new ImportAssignmentsCommand {
			def session = mockSession
		}
		command.assignmentImporter = importer
		command.assignmentMembershipService = membershipService

		val registrations: Seq[ModuleRegistration]
		importer.allMembers(any[ModuleRegistration=>Unit]) answers {
			case fn: (ModuleRegistration=>Unit) @unchecked => registrations.foreach(fn)
		}
	}

	behavior of "doGroupMembers"

	it should "process all collections" in {
		new Fixture {
			val registrations = Seq(
				ModuleRegistration("13/14", "0100001/1", "A", "HI33M-30", "A"),
				ModuleRegistration("13/14", "0100001/1", "A", "HI100-30", "A"),
				ModuleRegistration("13/14", "0100002/1", "A", "HI101-30", "A")
			)
			command.doGroupMembers()
			there were three(membershipService).replaceMembers(any[UpstreamAssessmentGroup], any[Seq[String]])
		}
	}

	/**
	 * TAB-1265
	 * Test doesn't currently check that it does anything special when no
	 * registrations exist for a module, because that's all it considers -
	 * need to change the command first so that it compares against the existing
	 * upstream assessment groups somehow.
	 */
	ignore should "process empty groups" in {
		new Fixture {
			val registrations = Seq(
				ModuleRegistration("13/14", "0100001/1", "A", "HI33M-30", "A"),
				ModuleRegistration("13/14", "0100002/1", "A", "HI33M-30", "A"),
				ModuleRegistration("13/14", "0100003/1", "A", "HI100-30", "A"),
				ModuleRegistration("13/14", "0100002/1", "A", "HI100-30", "A")
			)

			command.doGroupMembers()

			there was one(membershipService).replaceMembers(argThat(hasModuleCode("HI33M-30")), isEq(Seq("0100001", "0100002")))
			there was one(membershipService).replaceMembers(argThat(hasModuleCode("HI100-30")), isEq(Seq("0100003", "0100002")))

		}
	}

	/** Matches on an UpstreamAssessmentGroup's module code. */
	def hasModuleCode(code: String) = CustomHamcrestMatchers.hasProperty('moduleCode, code)


}
