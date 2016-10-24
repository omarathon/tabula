package uk.ac.warwick.tabula.commands.scheduling.imports

import org.hibernate.Session
import org.junit.runner.RunWith
import org.scalatest.junit._
import org.scalatest.{FlatSpec, Matchers}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup, UpstreamModuleRegistration}
import uk.ac.warwick.tabula.services.scheduling.AssignmentImporter
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.{AcademicYear, CustomHamcrestMatchers, Mockito}

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
		command.assessmentMembershipService = membershipService
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

		membershipService.getAssessmentComponents("HI33M-30") returns Seq(
			new AssessmentComponent {
				assessmentGroup = "A"
				sequence = "A01"
			},
			new AssessmentComponent {
				assessmentGroup = "A"
				sequence = "A02"
			}
		)
		membershipService.getAssessmentComponents("HI100-30") returns Seq(
			new AssessmentComponent {
				assessmentGroup = "A"
				sequence = "A01"
			}
		)
		membershipService.getAssessmentComponents("HI101-30") returns Seq(
			new AssessmentComponent {
				assessmentGroup = "A"
				sequence = "A01"
			}
		)

		val hi900_30 = {
			val g = new UpstreamAssessmentGroup
			g.id = "hi900_30"
			g.moduleCode = "HI900-30"
			g.occurrence = "A"
			g.assessmentGroup = "A"
			g.academicYear = AcademicYear.parse("13/14")
			g
		}
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
			verify(membershipService, times(4)).replaceMembers(any[UpstreamAssessmentGroup], any[Seq[UpstreamModuleRegistration]])
		}
	}

	/**
	 * TAB-1265
	 */
	it should "process empty groups" in {
		new Fixture {
			val registrations = Seq(
				UpstreamModuleRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "2", "A", "A01", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100003/1", "3", "A", "A01", "HI100-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "2", "A", "A01", "HI100-30", "A")
			)

			membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns Seq("hi900_30")

			command.doGroupMembers()

			verify(membershipService, times(2)).replaceMembers(anArgThat(hasModuleCode("HI33M-30")), isEq(
				Seq(
					registrations.head,
					registrations(1)
				)
			))

			verify(membershipService, times(1)).replaceMembers(anArgThat(hasModuleCode("HI100-30")), isEq(
				Seq(
					registrations(2),
					registrations(3)
				)
			))

			// The bug is that we don't update any group we don't have moduleregistrations for.
			verify(membershipService, times(0)).replaceMembers(anArgThat(hasModuleCode("HI900-30")), isEq(Nil))

		}
	}

	/**
	 * TAB-3389
	 */
	it should "set seat number to null where there is ambiguity" in {
		new Fixture {
			val registrations = Seq(
				UpstreamModuleRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100001/1", "1", "A", "A01", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "2", "A", "A01", "HI33M-30", "A"),
				UpstreamModuleRegistration("13/14", "0100002/1", "3", "A", "A01", "HI33M-30", "A")
			)

			membershipService.getUpstreamAssessmentGroupsNotIn(isEq(Seq("seenGroupId")), any[Seq[AcademicYear]]) returns Seq("hi900_30")

			command.doGroupMembers()

			// Duplicates now allowed and handled inside replaceMembers
			verify(membershipService, times(2)).replaceMembers(anArgThat(hasModuleCode("HI33M-30")), isEq(
				Seq(
					registrations.head,
					registrations(1),
					registrations(2),
					registrations(3)
				)
			))

			// 0100002/1 not passed in (stays null).
			// Only called once as it only matches the _exact_ group (where sequence is A01)
			// 0100001/1 set to 1 (duplicate seat number)
			verify(membershipService, times(1)).updateSeatNumbers(anArgThat(hasModuleCode("HI33M-30")), isEq(
				Map(
					"0100001" -> 1
				)
			))
		}
	}



	/** Matches on an UpstreamAssessmentGroup's module code. */
	def hasModuleCode(code: String) = CustomHamcrestMatchers.hasProperty('moduleCode, code)


}
