package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.commands.{DescriptionImpl, Description}
import uk.ac.warwick.tabula.JavaImports._


class EditSmallGroupCommandTest extends TestBase with Mockito {
	@Test
	def description() {
		val d = new DescriptionImpl
		val set = new SmallGroupSet
		val command = new EditSmallGroupCommandDescription with EditSmallGroupCommandState {
			val group = new SmallGroup
			val name = "Alan"
		}

		// cyclic relationship would caused JSON stack overflow if added directly to description
		command.group.id = "groupid123"
		command.group.groupSet = set
		set.id = "setid123"
		set.groups.add(command.group)

		command.describe(d)

		// mostly check that this doesn't stack overflow
		val result = json.writeValueAsString(d)

		d.allProperties should be (Map(
			"smallGroup" -> "groupid123",
			"smallGroupSet" -> "setid123",
			"name" -> "Alan")
		)
	}
}
