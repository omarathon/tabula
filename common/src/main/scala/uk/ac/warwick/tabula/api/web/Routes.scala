package uk.ac.warwick.tabula.api.web

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, RoleOverride}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.web.RoutesUtils

/**
  * Generates URLs to various locations, to reduce the number of places where URLs
  * are hardcoded and repeated.
  *
  * For methods called "apply", you can leave out the "apply" and treat the object like a function.
  */
object Routes {

  import RoutesUtils._

  private val context = "/api"
  private val defaultVersion = "/v1"

  object assignment {
    def apply(assignment: Assignment): String =
      s"$context$defaultVersion/module/${encoded(assignment.module.code)}/assignments/${encoded(assignment.id)}"
  }

  object department {

    object assignments {
      def apply(department: Department): String =
        s"$context$defaultVersion/department/${encoded(department.code)}/assignments"

      def xml(department: Department): String =
        s"$context$defaultVersion/department/${encoded(department.code)}/assignments.xml"
    }

  }

  object submission {
    def apply(submission: Submission): String =
      s"$context$defaultVersion/module/${encoded(submission.assignment.module.code)}/assignments/${encoded(submission.assignment.id)}/submissions/${encoded(submission.id)}"
  }

  object turnitin {
    def submitAssignmentCallback(assignment: Assignment): String =
      s"$context$defaultVersion/turnitin/turnitin-submit-assignment-response/assignment/${encoded(assignment.id)}"
  }

  object attachment {
    def apply(attachment: FileAttachment): String =
      s"$context$defaultVersion/attachments/${encoded(attachment.id)}"
  }

  object job {
    def apply(job: JobInstance): String =
      s"$context$defaultVersion/job/${encoded(job.id)}"
  }

  object timetables {
    def calendar(member: Member): String =
      s"$context$defaultVersion/member/${encoded(member.universityId)}/timetable/calendar"

    def calendarICal(member: Member): String =
      s"$context$defaultVersion/member/${encoded(member.universityId)}/timetable/calendar.ics"

    def calendarICalForHash(timetableHash: String): String =
      s"$context$defaultVersion/timetable/calendar/${encoded(timetableHash)}.ics"
  }

  object groupSet {
    def apply(groupSet: SmallGroupSet): String =
      s"$context$defaultVersion/module/${encoded(groupSet.module.code)}/groups/${encoded(groupSet.id)}"
  }

  object group {
    def apply(group: SmallGroup): String =
      s"$context$defaultVersion/module/${encoded(group.groupSet.module.code)}/groups/${encoded(group.groupSet.id)}/groups/${encoded(group.id)}"
  }

  object event {
    def apply(event: SmallGroupEvent): String =
      s"$context$defaultVersion/module/${encoded(event.group.groupSet.module.code)}/groups/${encoded(event.group.groupSet.id)}/groups/${encoded(event.group.id)}/events/${encoded(event.id)}"
  }

}
