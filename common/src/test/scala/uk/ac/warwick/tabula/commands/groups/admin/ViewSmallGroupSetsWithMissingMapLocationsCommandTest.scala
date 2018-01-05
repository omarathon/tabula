package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class ViewSmallGroupSetsWithMissingMapLocationsCommandTest extends TestBase with Mockito {
	@Test
	def listsEvents(): Unit = {
		val department = mock[Department]
		val year = AcademicYear(2017)
		val command = new ViewSmallGroupSetsWithMissingMapLocationsCommand(year, department) with SmallGroupServiceComponent {
			val smallGroupService: SmallGroupService = mock[SmallGroupService]
		}

		val set1 = Fixtures.smallGroupSet("set 1")
		set1.module = Fixtures.module("cs118")
		val set1Events = Seq(
			Fixtures.smallGroupEvent(""),
			Fixtures.smallGroupEvent(""),
			Fixtures.smallGroupEvent("")
		)

		val set2 = Fixtures.smallGroupSet("set 2")
		set2.module = Fixtures.module("cs101")
		val set2Events = Seq(
			Fixtures.smallGroupEvent(""),
			Fixtures.smallGroupEvent("")
		)

		val setEventsMap: Map[SmallGroupSet, Seq[SmallGroupEvent]] = Map(
			set1 -> set1Events,
			set2 -> set2Events
		)
		val setEventsSeq: Seq[(SmallGroupSet,  Seq[SmallGroupEvent])] = Seq(set2 -> set2Events, set1 -> set1Events)

		command.smallGroupService.listSmallGroupSetsWithEventsWithoutMapLocation(year, Some(department)) returns setEventsMap

		command.applyInternal() should equal(setEventsSeq)
	}
}
