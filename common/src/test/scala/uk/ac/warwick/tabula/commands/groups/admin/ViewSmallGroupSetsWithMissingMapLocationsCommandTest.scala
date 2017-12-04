package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}

class ViewSmallGroupSetsWithMissingMapLocationsCommandTest extends TestBase with Mockito {
	@Test
	def listsEvents(): Unit = {
		val department = mock[Department]
		val year = AcademicYear(2017)
		val command = new ViewSmallGroupSetsWithMissingMapLocationsCommand(year, department) with SmallGroupServiceComponent {
			val smallGroupService: SmallGroupService = mock[SmallGroupService]
		}

		val setEventsMap: Map[SmallGroupSet, Seq[SmallGroupEvent]] = Map(
			mock[SmallGroupSet] -> Seq(
				mock[SmallGroupEvent],
				mock[SmallGroupEvent],
				mock[SmallGroupEvent]
			),
			mock[SmallGroupSet] -> Seq(
				mock[SmallGroupEvent],
				mock[SmallGroupEvent]
			)
		)
		val setEventsSeq: Seq[(SmallGroupSet,  Seq[SmallGroupEvent])] = setEventsMap.toSeq

		command.smallGroupService.listSmallGroupSetsWithEventsWithoutMapLocation(year, Some(department)) returns setEventsMap

		command.applyInternal() should equal(setEventsSeq)
	}
}
