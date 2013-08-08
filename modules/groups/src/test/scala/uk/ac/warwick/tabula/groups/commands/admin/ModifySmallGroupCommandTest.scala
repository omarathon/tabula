package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.{UserGroup, Module}
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.TestBase

class ModifySmallGroupCommandTest extends TestBase {

	class TestableModifySmallGroupCommand(module: Module, properties: SmallGroupSetProperties) extends ModifySmallGroupCommand(module,properties){
		def describe(d: Description) {}
		protected def applyInternal(): SmallGroup = {null}
	}

  @Test
	def copyToDoesntCareAboutUserGroupType(){new SmallGroupFixture {
		val props = new SmallGroupSetProperties {}
		val command = new TestableModifySmallGroupCommand(groupSet1.module,  props)

		command.students = UserGroup.ofUniversityIds
		command.students.addUser("test")

		val targetGroup = new SmallGroup()
		targetGroup._studentsGroup = UserGroup.ofUsercodes //make sure it's different to the source
		command.copyTo(targetGroup)

		targetGroup._studentsGroup.universityIds should be(true)
		targetGroup._studentsGroup.members should be(Seq("test"))
	}
	}

	@Test
	def copyFromDoesntCareAboutUserGroupType(){new SmallGroupFixture {
		val props = new SmallGroupSetProperties {}
		val command = new TestableModifySmallGroupCommand(groupSet1.module,  props)

		group2._studentsGroup = UserGroup.ofUsercodes
		group2._studentsGroup.addUser("test")

		command.copyFrom(group2)
		command.students.universityIds should be(false)
		command.students.members should be(Seq("test"))

	}}
}
