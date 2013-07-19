package uk.ac.warwick.tabula.dev.web.commands

import scala.collection.JavaConversions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.scheduling.services.DepartmentInfo
import uk.ac.warwick.tabula.scheduling.services.ModuleInfo
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModulesCommand
import uk.ac.warwick.tabula.commands.permissions.GrantRoleCommand
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupFormat, SmallGroup, SmallGroupSet}

/** This command is intentionally Public. It only exists on dev and is designed,
  * in essence, to blitz a department and set up some sample data in it.
  */
class FixturesCommand extends Command[Unit] with Public with Daoisms {
	import ImportModulesCommand._

	var moduleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]

	def applyInternal() {
		setupDepartmentAndModules()

		// Two department admins
		val department = moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestDepartment.code).get

		val cmd = new GrantRoleCommand(department)
		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.addAll(Seq(Fixtures.TestAdmin1, Fixtures.TestAdmin2))
		cmd.apply()
	}

	private def setupDepartmentAndModules() {
		// Blitz the test department
		transactional() {
			moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestDepartment.code) map { dept =>
				for (module <- dept.modules) session.delete(module)
				for (feedbackTemplate <- dept.feedbackTemplates) session.delete(feedbackTemplate)
				for (markingWorkflow <- dept.markingWorkflows) session.delete(markingWorkflow)

				session.delete(dept)
			}
		}

		val department = newDepartmentFrom(Fixtures.TestDepartment)

		// Import a new, better department
		transactional() {
			session.save(department)
		}

		// Setup some modules in the department, deleting anything existing
		val moduleInfos = Seq(Fixtures.TestModule1, Fixtures.TestModule2, Fixtures.TestModule3)

		transactional() {
			for (modInfo <- moduleInfos; module <- moduleAndDepartmentService.getModuleByCode(modInfo.code)) {
				 session.delete(module)
			}
		}

		transactional() {
			for (modInfo <- moduleInfos)
				session.save(newModuleFrom(modInfo, department))
		}

	    // create a small group on the first module in the list
	    transactional(){
	      val firstModule = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule1.code).get
	      val groupSet = new SmallGroupSet()
	      groupSet.name="Test Lab"
	      groupSet.format = SmallGroupFormat.Lab
	      groupSet.module = firstModule
				groupSet.allocationMethod= SmallGroupAllocationMethod.Manual
	      val group  = new SmallGroup
	      group.name ="Test Lab Group 1"
	      groupSet.groups = JArrayList(group)
	      session.save(groupSet)
	    }

		  // and another, with AllocationMethod = "StudentSignUp", on the second
		transactional(){
			val secondModule = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule2.code).get
			val groupSet = new SmallGroupSet()
			groupSet.name="Module 2 Tutorial"
			groupSet.format = SmallGroupFormat.Tutorial
			groupSet.module = secondModule
			groupSet.allocationMethod= SmallGroupAllocationMethod.StudentSignUp
			val group  = new SmallGroup
			group.name ="Group 1"
			groupSet.groups = JArrayList(group)
			session.save(groupSet)
		}

		session.flush()
		session.clear()
	}

	def describe(d: Description) {}

}

object Fixtures {
	val TestDepartment = DepartmentInfo("Test Services", "xxx", null)

	val TestModule1 = ModuleInfo("Test Module 1", "xxx101", "xxx-xxx101")
	val TestModule2 = ModuleInfo("Test Module 2", "xxx102", "xxx-xxx102")
	val TestModule3 = ModuleInfo("Test Module 3", "xxx103", "xxx-xxx103")


	val TestAdmin1 = "tabula-functest-admin1"
	val TestAdmin2 = "tabula-functest-admin2"
}