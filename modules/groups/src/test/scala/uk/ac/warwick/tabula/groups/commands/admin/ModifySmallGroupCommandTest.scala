package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.{UserGroup, Module}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroup}
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.TestBase

class ModifySmallGroupCommandTest extends TestBase {

	class TestableModifySmallGroupCommand(module: Module, properties: SmallGroupSetProperties) extends ModifySmallGroupCommand(module,properties){
		def describe(d: Description) {}
		protected def applyInternal(): SmallGroup = {null}
	}

  @Test
	def copyToOverwritesEvents() {
		new SmallGroupFixture {
			val props = new SmallGroupSetProperties {}
			val command = new TestableModifySmallGroupCommand(groupSet1.module,  props)

			val targetGroup = new SmallGroup()
			targetGroup.events.add(new SmallGroupEvent) // make sure it's different to the source
			targetGroup.events.size should be(1)

			command.copyTo(targetGroup)

			targetGroup.events.size should be(0)
		}
	}

	@Test
	def copyFromOverwritesEvents() {
		new SmallGroupFixture {
			val props = new SmallGroupSetProperties {}
			val command = new TestableModifySmallGroupCommand(groupSet1.module,  props)

			command.copyFrom(group2)
			command.events.size should be(1)
			command.events.get(0).location should be("CMR0.1")
		}
	}
}
