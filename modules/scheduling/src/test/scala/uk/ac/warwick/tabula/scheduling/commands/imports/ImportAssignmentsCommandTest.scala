package uk.ac.warwick.tabula.scheduling.commands.imports

import org.hibernate.Session
import org.scalatest.junit._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith

import uk.ac.warwick.tabula.{AcademicYear, CustomHamcrestMatchers, Mockito}
import uk.ac.warwick.tabula.scheduling.services.AssignmentImporter
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, AssessmentMembershipService}
import uk.ac.warwick.tabula.data.model.{UpstreamModuleRegistration, UpstreamAssessmentGroup}

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

		moduleService.getModuleByCode(any[String]) returns None // Not necessary for this to work
		membershipService.replaceMembers(any[UpstreamAssessmentGroup], any[Seq[UpstreamModuleRegistration]]) answers { args =>
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
			membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns Nil

			val registrations = Seq(
				UpstreamModuleRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100001/1", "1", "A", "A01", "HI100-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "2", "A", "A01", "HI101-30", "A")
			)
			command.doGroupMembers()
			verify(membershipService, times(3)).replaceMembers(any[UpstreamAssessmentGroup], any[Seq[UpstreamModuleRegistration]])
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
				UpstreamModuleRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "2", "A", "A01", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100003/1", "3", "A", "A01", "HI100-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "2", "A", "A01", "HI100-30", "A")
			)

			membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns Seq(hi900_30)

			command.doGroupMembers()

			verify(membershipService, times(1)).replaceMembers(anArgThat(hasModuleCode("HI33M-30")), isEq(
				Seq(
					registrations(1),
					registrations(0)
				)
			))

			verify(membershipService, times(1)).replaceMembers(anArgThat(hasModuleCode("HI100-30")), isEq(
				Seq(
					registrations(3),
					registrations(2)

				)
			))

			// The bug is that we don't update any group we don't have moduleregistrations for.
			verify(membershipService, times(1)).replaceMembers(anArgThat(hasModuleCode("HI900-30")), isEq(Nil))

		}
	}

	/**
	 * TAB-3388
	 */
	it should "change seat number to null where it is ambiguous" in {
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
				UpstreamModuleRegistration("13/14", "0100001/1", "1", "A", "A01","HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100001/1", "10", "A","A01", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "2", "A", "A01","HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100003/1", "3", "A", "A01","HI100-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "2", "A", "A01","HI100-30", "A")
			)

			membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns Seq(hi900_30)

			command.doGroupMembers()

			verify(membershipService, times(1)).replaceMembers(anArgThat(hasModuleCode("HI33M-30")), isEq(
				Seq(
					registrations(2),
					UpstreamModuleRegistration("13/14","0100001/1",null,"A", "A01", "HI33M-30","A")
				)
			))

			verify(membershipService, times(1)).replaceMembers(anArgThat(hasModuleCode("HI100-30")), isEq(
				Seq(
					registrations(4),
					registrations(3)
				)
			))
		}
	}

	/** Matches on an UpstreamAssessmentGroup's module code. */
	def hasModuleCode(code: String) = CustomHamcrestMatchers.hasProperty('moduleCode, code)


}
