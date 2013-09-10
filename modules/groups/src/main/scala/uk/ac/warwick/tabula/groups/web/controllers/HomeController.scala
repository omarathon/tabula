package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.{SmallGroupService, ModuleAndDepartmentService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.StudentSignUp
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewSet
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModule
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModules

@Controller class HomeController extends GroupsController {
	import HomeController._
	var moduleService = Wire[ModuleAndDepartmentService]
	var smallGroupService = Wire[SmallGroupService]



	@RequestMapping(Array("/")) def home(user: CurrentUser) = {

		val ownedDepartments = moduleService.departmentsWithPermission(user, Permissions.Module.ManageSmallGroups)
		val ownedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageSmallGroups)
		val taughtGroups = smallGroupService.findSmallGroupsByTutor(user.apparentUser)
		val memberGroupSets = smallGroupService.findSmallGroupSetsByMember(user.apparentUser)
		val releasedMemberGroupSets = getGroupSetsReleasedToStudents(memberGroupSets)
		val nonEmptyMemberViewModules = getViewModulesForStudent(releasedMemberGroupSets,getGroupsToDisplay(_,user.apparentUser))

		Mav("home/view",
			"ownedDepartments" -> ownedDepartments,
			"ownedModuleDepartments" -> ownedModules.map { _.department },
			"taughtGroups" -> taughtGroups,
			"memberGroupsetModules"->ViewModules(nonEmptyMemberViewModules, false)
		)
	}

}
//
// Stateless functions to munge groupsets and groups
//
object HomeController {
	// For each of the groupsets of which this user is a member, work out which of the constituent groups
	// should be displayed (which could be None, All, or "All groups the student is allocated to"), based
	// on the type and state of the groupset. Also return whether the student is allocated to the returned
	// groups or not.
	//
	// c.f rules outlined in TAB-922
	//
	def getGroupsToDisplay(set:SmallGroupSet, user:User):(Seq[SmallGroup],ViewerRole) = {
		val allGroupsInSet = set.groups.asScala.toSeq
		val groupsStudentHasJoined = allGroupsInSet.filter(_.students.users.contains(user))
		groupsStudentHasJoined match {
			case Nil if (set.allocationMethod == StudentSignUp && set.openForSignups == false) => (Nil,StudentNotAssignedToGroup)
			case Nil if (set.allocationMethod == StudentSignUp)                                => (allGroupsInSet, StudentNotAssignedToGroup)
			case Nil                                                                           => (Nil,StudentNotAssignedToGroup)
			case x:Seq[SmallGroup]                                                             => (x, StudentAssignedToGroup)
		}
	}

	// Given a list of groupsets (which a student is a member of), group them by module and then
	// return all the modules with at least one groupset with at least one group that needs
	// to be displayed to the student (@see getGroupsToDisplay); map the modules and groupsets to
	// ViewModules and ViewSets for rendering.

	def getViewModulesForStudent(memberGroupSets:Seq[SmallGroupSet], getDisplayGroups:(SmallGroupSet)=>(Seq[SmallGroup],ViewerRole) ):Seq[ViewModule]={
		val memberGroupSetsWithApplicableGroups = memberGroupSets.map(set=>(set,getDisplayGroups(set))).filterNot{case(s,(groups,role))=>groups.isEmpty}

		val memberViewModules =	memberGroupSetsWithApplicableGroups.groupBy(_._1.module).map{
			case(module,setData)=>{
				val viewSets = setData.map{case(set,(groups,role))=>{
					ViewSet(set,groups,role)
				}}
				ViewModule(module,viewSets,false)
			}
		}
		memberViewModules.filterNot(m=>m.setItems.isEmpty).toSeq
	}

	def getGroupSetsReleasedToStudents(memberGroupSets:Seq[SmallGroupSet]) = memberGroupSets.filter(s => (s.releasedToStudents || s.allocationMethod == StudentSignUp))

}