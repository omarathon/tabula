package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSetFilters
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, SmallGroupService}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class SmallGroupSetFilterConverterTest extends TestBase with Mockito {

	val converter = new SmallGroupSetFilterConverter
	converter.moduleService = smartMock[ModuleAndDepartmentService]
	converter.smallGroupService = smartMock[SmallGroupService]

	@Test def convertRightModule(): Unit = {
		val module = Fixtures.module(code = "cs118", name = "Programming for Computer Scientists")
		converter.moduleService.getModuleByCode("cs118") returns Some(module)

		converter.convertRight("Module(cs118)") should be (SmallGroupSetFilters.Module(module))
		converter.convertRight("Module(CS118)") should be (SmallGroupSetFilters.Module(module))
	}

	@Test def convertRightDepartmentSmallGroup(): Unit = {
		val dsgs = Fixtures.departmentSmallGroupSet("First year seminar groups")
		converter.smallGroupService.getDepartmentSmallGroupSetById("sgs-id") returns Some(dsgs)

		converter.convertRight("AllocationMethod.Linked(sgs-id)") should be (SmallGroupSetFilters.AllocationMethod.Linked(dsgs))
	}

	@Test def convertRightTerm(): Unit = {
		val all = SmallGroupSetFilters.allTermFilters(AcademicYear.now())
		all.foreach { filter =>
			converter.convertRight(filter.getName) should be (filter)
		}
	}

	@Test def convertRightFormat(): Unit = {
		val all = SmallGroupSetFilters.allFormatFilters
		all.foreach { filter =>
			converter.convertRight(filter.getName) should be (filter)
		}
	}

	@Test def convertRightOthers(): Unit = {
		val statusFilters = SmallGroupSetFilters.Status.all
		statusFilters.foreach { filter =>
			converter.convertRight(filter.getName) should be (filter)
		}

		converter.convertRight("AllocationMethod.ManuallyAllocated") should be (SmallGroupSetFilters.AllocationMethod.ManuallyAllocated)
		converter.convertRight("AllocationMethod.StudentSignUp") should be (SmallGroupSetFilters.AllocationMethod.StudentSignUp)
	}

	@Test def convertLeft(): Unit = {
		val module = Fixtures.module(code = "cs118", name = "Programming for Computer Scientists")
		converter.convertLeft(SmallGroupSetFilters.Module(module)) should be ("Module(cs118)")

		val dsgs = Fixtures.departmentSmallGroupSet("First year seminar groups")
		dsgs.id = "sgs-id"
		converter.convertLeft(SmallGroupSetFilters.AllocationMethod.Linked(dsgs)) should be ("AllocationMethod.Linked(sgs-id)")
		converter.convertLeft(SmallGroupSetFilters.AllocationMethod.ManuallyAllocated) should be ("AllocationMethod.ManuallyAllocated")
		converter.convertLeft(SmallGroupSetFilters.AllocationMethod.StudentSignUp) should be ("AllocationMethod.StudentSignUp")

		SmallGroupSetFilters.allTermFilters(AcademicYear(2017)).map(converter.convertLeft) should be (Seq("Term(Pre-term vacation, -8, 0)", "Term(Autumn, 1, 10)", "Term(Christmas vacation, 11, 14)", "Term(Spring, 15, 24)", "Term(Easter vacation, 25, 29)", "Term(Summer, 30, 39)", "Term(Summer vacation, 40, 52)"))
		SmallGroupSetFilters.allFormatFilters.map(converter.convertLeft) should be (Seq("seminar", "lab", "tutorial", "project", "example", "workshop", "lecture", "exam", "meeting"))
		SmallGroupSetFilters.Status.all.map(converter.convertLeft) should be (Seq("Status.NeedsGroupsCreating", "Status.UnallocatedStudents", "Status.NeedsEventsCreating", "Status.OpenForSignUp", "Status.ClosedForSignUp", "Status.NeedsNotificationsSending", "Status.StudentsDeregistered", "Status.Completed"))
	}

}
