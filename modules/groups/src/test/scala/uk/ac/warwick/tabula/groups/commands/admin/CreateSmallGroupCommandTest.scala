package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.{Fixtures, Mockito, AcademicYear, TestBase}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.Promises
import org.mockito.Mockito._
import uk.ac.warwick.userlookup.User


class CreateSmallGroupCommandTest extends TestBase with Mockito {

	@Test
	def UsesDefaultMaxGroupSizeIfEnabled (){

		val module = Fixtures.module("in101", "Set Theory")
		val set = Fixtures.smallGroupSet("My small groups")
		set.defaultMaxGroupSize = 9
		set.defaultMaxGroupSizeEnabled = false
		val promise = Promises.promise(set)
		val properties = mock[ModifySmallGroupSetCommand]
		when(properties.defaultMaxGroupSize).thenReturn(8)
		when(properties.defaultMaxGroupSizeEnabled).thenReturn(true)

		val command = new CreateSmallGroupCommand(promise, module, properties)

		val newGroup = command.applyInternal()
		newGroup.maxGroupSize should be(Some(8))
		newGroup.maxGroupSizeEnabled should be(true)

	}

	@Test
	def DoesntUseDefaultMaxGroupSizeIfNotEnabled (){

		val module = Fixtures.module("in101", "Set Theory")
		val set = Fixtures.smallGroupSet("My small groups")
		set.defaultMaxGroupSize = 9
		set.defaultMaxGroupSizeEnabled = false
		val promise = Promises.promise(set)
		val properties = mock[ModifySmallGroupSetCommand]
		when(properties.defaultMaxGroupSize).thenReturn(8)
		when(properties.defaultMaxGroupSizeEnabled).thenReturn(false)

		val command = new CreateSmallGroupCommand(promise, module, properties)

		val newGroup = command.applyInternal()
		newGroup.maxGroupSize should be(Some(SmallGroup.DefaultGroupSize))
		newGroup.maxGroupSizeEnabled should be (false)

	}

}
