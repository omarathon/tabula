package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.data.SessionComponent
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportModulesCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.JavaImports.JArrayList

class SmallGroupSetFixtureCommand extends CommandInternal[Unit] with Logging {

	this: ModuleAndDepartmentServiceComponent with SessionComponent =>

	var moduleCode:String = _
	var groupSetName:String = _
	var formatName:String = _
	var allocationMethodName:String = _
	var groupCount:Int = _

	protected def applyInternal() {
		transactional(){
			moduleAndDepartmentService.getModuleByCode(moduleCode) match {
				case Some(module)=>{
					val groupSet = new SmallGroupSet()
					groupSet.name=groupSetName
					groupSet.format = SmallGroupFormat.fromCode(formatName)
					groupSet.module = module
					groupSet.allocationMethod= SmallGroupAllocationMethod.fromDatabase(allocationMethodName)
					groupSet.groups =JArrayList()
						(1 to groupCount).foreach{i=>
						val group  = new SmallGroup
						group.name =s"Group $i"
						groupSet.groups.add(group)
					}
					session.save(groupSet)
				}
				case _ => throw new RuntimeException
			}
			}
	}
}
