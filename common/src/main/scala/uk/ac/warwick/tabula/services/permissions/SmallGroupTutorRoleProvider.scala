package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent}
import uk.ac.warwick.tabula.helpers.MutablePromise
import uk.ac.warwick.tabula.helpers.Promises.promise
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.services.SmallGroupService

@Component
class SmallGroupTutorRoleProvider extends RoleProvider
	with CustomRolesForAdminDepartments
	with TaskBenchmarking {

	val smallGroupService: MutablePromise[SmallGroupService] = promise { Wire[SmallGroupService] }

	override def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = benchmarkTask("Get roles for SmallGroupTutorRoleProvider") {
		scope match {
			case event: SmallGroupEvent => getRoles(user, Seq(event))
			case group: SmallGroup => getRoles(user, group.events)

			case student: StudentMember =>
				// TAB-5745
				val studentGroups = smallGroupService.get.findSmallGroupsByStudent(student.asSsoUser)
				val isTutor =
					studentGroups.exists { group =>
						group.groupSet.releasedToTutors && group.events.exists(_.tutors.includesUser(user.apparentUser))
					}

				if (isTutor) {
					val custom = customRoles(student, SmallGroupMembersTutorRoleDefinition)
					Stream(custom.headOption.getOrElse(SmallGroupMembersTutor(student)))
				} else Stream.empty

			case _ => Stream.empty
		}
	}

	private def getRoles(user: CurrentUser, events: Iterable[SmallGroupEvent]) = {
		val validEvents =
			events.toStream
				.filter { _.group.groupSet.releasedToTutors }
				.filter { _.tutors.includesUser(user.apparentUser) }

		val eventRoles = validEvents.map { event =>
			customRoleFor(event.group.groupSet.module.adminDepartment)(SmallGroupEventTutorRoleDefinition, event).getOrElse(SmallGroupEventTutor(event))
		}

		val groupRoles = validEvents.map { _.group }.distinct.map { group =>
			customRoleFor(group.groupSet.module.adminDepartment)(SmallGroupTutorRoleDefinition, group).getOrElse(SmallGroupTutor(group))
		}

		eventRoles #::: groupRoles
	}

	def rolesProvided = Set(classOf[SmallGroupTutor], classOf[SmallGroupEventTutor], classOf[SmallGroupMembersTutor])

}