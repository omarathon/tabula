package uk.ac.warwick.tabula.dev.web.commands

import scala.reflect._
import scala.collection.JavaConversions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, RelationshipService}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportAcademicInformationCommand
import uk.ac.warwick.tabula.commands.permissions.GrantRoleCommand
import uk.ac.warwick.tabula.roles.{UserAccessMgrRoleDefinition, DepartmentalAdministratorRoleDefinition}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupFormat, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import org.hibernate.criterion.Restrictions
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.scheduling.services.ModuleInfo
import uk.ac.warwick.tabula.scheduling.services.DepartmentInfo
import uk.ac.warwick.tabula.permissions.PermissionsTarget

/** This command is intentionally Public. It only exists on dev and is designed,
  * in essence, to blitz a department and set up some sample data in it.
  */
class FixturesCommand extends Command[Unit] with Public with Daoisms {
	import ImportAcademicInformationCommand._

	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
	var routeDao = Wire[RouteDao]
	var relationshipService = Wire[RelationshipService]
	var scdDao = Wire[StudentCourseDetailsDao]
	var memberDao = Wire[MemberDao]
	var monitoringPointDao = Wire[MonitoringPointDao]
	var attendanceMonitoringDao = Wire[AttendanceMonitoringDao]
	var permissionsService = Wire[PermissionsService]

	def applyInternal() {
		setupDepartmentAndModules()

		val department = moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestDepartment.code).get
		val subDept = moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestSubDepartment.code).get
		val subSubDept = moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestSubSubDepartment.code).get

		val module1 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule1.code).get
		val module2 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule2.code).get
		val module3 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule3.code).get
		val module4 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule4.code).get

		// Two department admins, first is a UserAccessManager
		val cmd = GrantRoleCommand(department)

		cmd.roleDefinition = UserAccessMgrRoleDefinition
		cmd.usercodes.add(Fixtures.TestAdmin1)
		cmd.apply()


		cmd.roleDefinition = DepartmentalAdministratorRoleDefinition
		cmd.usercodes.clear()
		cmd.usercodes.add(Fixtures.TestAdmin2)
		cmd.apply()

		// admin on the sub-department
		val subDepartmentAdminCommand = GrantRoleCommand(subDept)
		subDepartmentAdminCommand.roleDefinition = DepartmentalAdministratorRoleDefinition
		subDepartmentAdminCommand.usercodes.addAll(Seq(Fixtures.TestAdmin3))
		subDepartmentAdminCommand.apply()

		// admin on the sub-department;
		val subSubDepartmentAdminCommand = GrantRoleCommand(subSubDept)
		subSubDepartmentAdminCommand.roleDefinition = DepartmentalAdministratorRoleDefinition
		subSubDepartmentAdminCommand.usercodes.addAll(Seq(Fixtures.TestAdmin4))
		subSubDepartmentAdminCommand.apply()


		val upstreamAssignment = new AssessmentComponent
		upstreamAssignment.assessmentGroup = "A"
		upstreamAssignment.sequence = "A"
		upstreamAssignment.moduleCode = "XXX101-30"
		upstreamAssignment.module = module1
		upstreamAssignment.name = "Assignment from SITS"
		upstreamAssignment.assessmentType = AssessmentType.Assignment
		session.save(upstreamAssignment)

		val upstreamAssessmentGroup = new UpstreamAssessmentGroup
		upstreamAssessmentGroup.academicYear = new AcademicYear(new DateTime().getYear)
		upstreamAssessmentGroup.moduleCode = "XXX101-30"
		upstreamAssessmentGroup.assessmentGroup = "A"
		upstreamAssessmentGroup.occurrence = "A"
		session.save(upstreamAssessmentGroup)
	}

	private def setupDepartmentAndModules() {
		// Blitz members
		transactional() {
			sessionWithoutFreshFilters.newQuery("delete from StudentCourseYearDetails where studentCourseDetails.scjCode like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from ModuleRegistration where studentCourseDetails.scjCode like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from MemberStudentRelationship where studentCourseDetails.scjCode like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from StudentCourseDetails where scjCode like '3000%'").executeUpdate()
			sessionWithoutFreshFilters.newQuery("delete from StudentMember where universityId like '3000%'").executeUpdate()
		}
		
		// Blitz the test department
		transactional() {
			moduleAndDepartmentService.getDepartmentByCode(Fixtures.TestDepartment.code) map { dept =>
				val routes: Seq[Route] = routeDao.findByDepartment(dept)

				val schemes = attendanceMonitoringDao.listAllSchemes(dept)

				val deptScds = scdDao.findByDepartment(dept)
				val scds = deptScds.distinct

				for (scd <- scds) {
					for (mr <- scd.moduleRegistrations) {
						session.delete(mr)
					}
					scd.clearModuleRegistrations()
				}

				// Checkpoints must be deleted before the associated student
				for (route <- routes) {
					val sets = monitoringPointDao.findMonitoringPointSets(route)
					for (set <- sets) {
						for (point <- set.points) {
							for (checkpoint <- point.checkpoints) session.delete(checkpoint)
							session.delete(point)
						}
						session.delete(set)
					}
				}

				for (scheme <- schemes) {
					for (point <- scheme.points){
						for (checkpoint <- attendanceMonitoringDao.getAllCheckpoints(point)){
							session.delete(checkpoint)
						}
					}
					// the points will also be deleted by the cascade
					session.delete(scheme)
				}

				for (total <- attendanceMonitoringDao.getAllCheckpointTotals(dept)) {
					session.delete(total)
				}

			  for (student <- scds.map{ _.student}.distinct) {
					//should cascade delete SCDs too
					session.delete(student)
				}

				for (staff <- memberDao.getStaffByDepartment(dept)) {
					session.delete(staff)
				}

				def invalidateAndDeletePermissions[A <: PermissionsTarget : ClassTag](scope: A) {
					// Invalidate any permissions or roles set
					val usersToInvalidate =
						permissionsService.getAllGrantedRolesFor(scope).flatMap { role => role.users.users } ++
						permissionsService.getAllGrantedPermissionsFor(scope).flatMap { perm => perm.users.users }

					usersToInvalidate.foreach { user =>
						permissionsService.clearCachesForUser((user.getUserId, classTag[A]))
					}

					permissionsService.getAllGrantedRolesFor(scope).foreach(session.delete)
					permissionsService.getAllGrantedPermissionsFor(scope).foreach(session.delete)
				}

				val modules = dept.modules
				modules.foreach(invalidateAndDeletePermissions[Module])
				modules.foreach(session.delete)
				dept.modules.clear()

				for (feedbackTemplate <- dept.feedbackTemplates) session.delete(feedbackTemplate)
				dept.feedbackTemplates.clear()

				for (markingWorkflow <- dept.markingWorkflows) session.delete(markingWorkflow)
				dept.markingWorkflows.clear()

				routes.foreach(invalidateAndDeletePermissions[Route])
				routes.foreach(session.delete)
				dept.routes.clear()

				val children = recursivelyGetChildren(dept)
				children.foreach(invalidateAndDeletePermissions[Department])
				children.foreach(session.delete)
				dept.children.clear()

				invalidateAndDeletePermissions[Department](dept)
				session.delete(dept)
			}
		}
		def recursivelyGetChildren(department:Department): Set[Department] = {
			val descendents = department.children flatMap { recursivelyGetChildren }
			descendents.toSet ++ department.children
		}

		val department = newDepartmentFrom(Fixtures.TestDepartment, moduleAndDepartmentService)

		// make sure we can see names, as uni ids are not exposed in the fixtures
		department.showStudentName = true
		transactional() {
			session.newCriteria[AssessmentComponent]
				.add(Restrictions.in("departmentCode", JList("xxx","XXX")))
				.list
				.foreach { ua => session.delete(ua); }

			session.newCriteria[UpstreamAssessmentGroup]
				.add(Restrictions.like("moduleCode", "XXX%"))
				.seq
				.foreach { uag => session.delete(uag); }
		}

		// Import a new, better department
		transactional() {
			session.save(department)
		}
		// make sure the new parent department is flushed to the DB before we fetch it to create the child
		session.flush()

		val subDepartment = newDepartmentFrom(Fixtures.TestSubDepartment, moduleAndDepartmentService)
		transactional() {
			session.save(subDepartment)
		}
		val subSubDepartment = newDepartmentFrom(Fixtures.TestSubSubDepartment, moduleAndDepartmentService)
			transactional() {
				session.save(subSubDepartment)
		}

		// Setup some modules in the department, deleting anything existing
		val moduleInfos = Seq(Fixtures.TestModule1, Fixtures.TestModule2, Fixtures.TestModule3)

		transactional() {
			for (modInfo <- moduleInfos; module <- moduleAndDepartmentService.getModuleByCode(modInfo.code)) {
				 session.delete(module)
			}
			val module4 = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule4.code)
			module4 map session.delete
		}

		transactional() {
			for (modInfo <- moduleInfos)
				session.save(newModuleFrom(modInfo, department))
			session.save(newModuleFrom(Fixtures.TestModule4, subDepartment))
		}

	    // create a small group on the first module in the list
	    transactional() {
	      val firstModule = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule1.code).get
	      val groupSet = new SmallGroupSet()
	      groupSet.name = "Test Lab"
	      groupSet.format = SmallGroupFormat.Lab
	      groupSet.module = firstModule
		  groupSet.allocationMethod= SmallGroupAllocationMethod.Manual
	      val group  = new SmallGroup
	      group.name ="Test Lab Group 1"
	      groupSet.groups = JArrayList(group)
	      session.save(groupSet)
	    }

		  // and another, with AllocationMethod = "StudentSignUp", on the second
		transactional() {
			val secondModule = moduleAndDepartmentService.getModuleByCode(Fixtures.TestModule2.code).get
			val groupSet = new SmallGroupSet()
			groupSet.name = "Module 2 Tutorial"
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
	val TestSubDepartment = DepartmentInfo("Test Services - Undergraduates", "xxx-ug", null,Some("xxx"),Some("UG"))
	val TestSubSubDepartment = DepartmentInfo("Test Services - Freshers", "xxx-ug1", null,Some("xxx-ug"),Some("UG,Y1"))

	val TestModule1 = ModuleInfo("Test Module 1", "xxx101", "xxx-xxx101")
	val TestModule2 = ModuleInfo("Test Module 2", "xxx102", "xxx-xxx102")
	val TestModule3 = ModuleInfo("Test Module 3", "xxx103", "xxx-xxx103")
	val TestModule4 = ModuleInfo("Test Module 3","xxx-ug-104","xxx-ug-xxx-ug-104")


	val TestAdmin1 = "tabula-functest-admin1"
	val TestAdmin2 = "tabula-functest-admin2"
	val TestAdmin3 = "tabula-functest-admin3"
	val TestAdmin4 = "tabula-functest-admin4"
}
