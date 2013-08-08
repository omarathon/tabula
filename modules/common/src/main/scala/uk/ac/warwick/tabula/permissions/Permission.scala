package uk.ac.warwick.tabula.permissions

import uk.ac.warwick.tabula.CaseObjectEqualityFixes

sealed abstract class Permission(val description: String) extends CaseObjectEqualityFixes[Permission] {
	val getName = Permissions.shortName(getClass.asInstanceOf[Class[_ <: Permission]])

	val isScoped = true
}
sealed abstract class ScopelessPermission(description: String) extends Permission(description) {
	override val isScoped = false
}

case class CheckablePermission(val permission: Permission, val scope: Option[PermissionsTarget])

object CheckablePermission {
	def apply(permission: ScopelessPermission): CheckablePermission = new CheckablePermission(permission, None)
	def apply(permission: Permission, scope: PermissionsTarget): CheckablePermission = new CheckablePermission(permission, Some(scope))
}

/* To avoid nasty namespace/scope clashes, stick all of this in a Permission object */
object Permissions {

	private val ObjectClassPrefix = Permissions.getClass.getName

	/**
	 * Create an Permission from an action name (e.g. "Module.Create").
	 * Most likely useful in view templates, for permissions checking.
	 *
	 * Note that, like the templates they're used in, the correctness isn't
	 * checked at runtime.
	 */
	def of(name: String): Permission = {
		try {
			// Go through the magical hierarchy
			val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
			clz.getDeclaredField("MODULE$").get(null).asInstanceOf[Permission]
		} catch {
			case e: ClassNotFoundException => throw new IllegalArgumentException("Permission " + name + " not recognised")
			case e: ClassCastException => throw new IllegalArgumentException("Permission " + name + " is not an endpoint of the hierarchy")
		}
	}

	def shortName(clazz: Class[_ <: Permission])
		= clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')

	/* ScopelessPermissions are Permissions that can be resolved without having to worry about scope */
	case object UserPicker extends ScopelessPermission("Use the user picker")

	case object Masquerade extends ScopelessPermission("Masquerade as other users")
	case object GodMode extends ScopelessPermission("Enable god mode")
	case object ManageMaintenanceMode extends ScopelessPermission("Manage maintenance mode settings")
	case object ImportSystemData extends ScopelessPermission("Import data from other systems")
	case object ReplicaSyncing extends ScopelessPermission("Manually run replica syncing")

	object RolesAndPermissions {
		case object Create extends Permission("Add roles and permissions")
		case object Read extends Permission("View roles and permissions")
		case object Update extends Permission("Edit roles and permissions")
		case object Delete extends Permission("Remove roles and permissions")
	}

	object Department {
		case object ArrangeModules extends Permission("Sort modules into sub-departments")
		case object ManageExtensionSettings extends Permission("Manage extension settings")
		case object ManageDisplaySettings extends Permission("Manage display settings")
		case object DownloadFeedbackReport extends Permission("Generate a feedback report")
		case object ManageProfiles extends Permission("Manage student profiles")
	}

	object Module {
		// We don't Read a module, we ManageAssignments on it
		case object ManageAssignments extends Permission("Manage assignments")
		case object ManageSmallGroups extends Permission("Manage small groups")

		case object Create extends Permission("Add a module")
		case object Update extends Permission("Edit a module")
		case object Delete extends Permission("Remove a module")
	}

	object Assignment {
		case object ImportFromExternalSystem extends Permission("Import assignments from SITS")
		case object Archive extends Permission("Archive an assignment")

		case object Create extends Permission("Add an assignment")
		case object Read extends Permission("View an assignment's settings")
		case object Update extends Permission("Edit an assignment")
		case object Delete extends Permission("Delete an assignment")
	}

	object Submission {
		case object ViewPlagiarismStatus extends Permission("View plagiarism status for a submission")
		case object ManagePlagiarismStatus extends Permission("Manage a submission's plagiarism status")
		case object CheckForPlagiarism extends Permission("Check a submission for plagiarism")
		case object SendReceipt extends Permission("Send a receipt for a submission")
		case object ReleaseForMarking extends Permission("Release a submission for marking")

		case object Create extends Permission("Add a submission")
		case object Read extends Permission("View a submission")
		case object Update extends Permission("Edit a submission")
		case object Delete extends Permission("Remove a submission")
	}

	object Feedback {
		case object Publish extends Permission("Release feedback to a student")
		case object Rate extends Permission("Rate feedback received")

		case object Create extends Permission("Add feedback")
		case object Read extends Permission("View feedback")
		case object Update extends Permission("Edit feedback")
		case object Delete extends Permission("Remove feedback")
	}

	object Marks {
		case object DownloadTemplate extends Permission("Download a marks template")

		case object Create extends Permission("Add marks")
		case object Read extends Permission("View marks")
		case object Update extends Permission("Edit marks")
		case object Delete extends Permission("Remove marks")
	}

	object Extension {
		case object MakeRequest extends Permission("Make an extension request")
		case object ReviewRequest extends Permission("Review an extension request")

		case object Create extends Permission("Make an extension request")
		case object Read extends Permission("View an extension request")
		case object Update extends Permission("Edit an extension request")
		case object Delete extends Permission("Remove an extension request")
	}

	object FeedbackTemplate {
		case object Create extends Permission("Add a feedback template")
		case object Read extends Permission("View a feedback template")
		case object Update extends Permission("Edit a feedback template")
		case object Delete extends Permission("Remove a feedback template")
	}

	object MarkingWorkflow {
		case object Create extends Permission("Add a marking workflow")
		case object Read extends Permission("View a marking workflow")
		case object Update extends Permission("Edit a marking workflow")
		case object Delete extends Permission("Delete a marking workflow")
	}

	object Profiles {
		case object Search extends ScopelessPermission("Search student profiles")

		object Read {
			case object Core extends Permission("View a member's photo, name, Warwick email, job title and University number")
			case object DateOfBirth extends Permission("View a member's date of birth")
			case object Nationality extends Permission("View a member's nationality")
			case object Gender extends Permission("View a member's gender")
			case object NextOfKin extends Permission("View a member's next of kin")
			case object HomeAddress extends Permission("View a member's home address")
			case object TermTimeAddress extends Permission("View a member's term-time address")
			case object TelephoneNumber extends Permission("View a member's telephone number")
			case object MobileNumber extends Permission("View a member's mobile number")
			case object HomeEmail extends Permission("View a member's alternative email address")
			case object Usercode extends Permission("View a member's usercode")
			case object SmallGroups extends Permission("View a member's small groups")

			object StudentCourseDetails {
				case object Core extends Permission("View a student's basic course, route and department details")
				case object Status extends Permission("View a student's enrolment and study status")
			}
			
			case object PersonalTutees extends Permission("View a member's personal tutees")
			case object Supervisees extends Permission("View a member's supervisees")
		}

		// Person's own tutor ('upward' relationship)
		object PersonalTutor {
			case object Upload extends Permission("Upload personal tutors from a spreadsheet")

			case object Create extends Permission("Add a personal tutor")
			case object Read extends Permission("View a personal tutor")
			case object Update extends Permission("Edit a personal tutor")
			case object Delete extends Permission("Remove a personal tutor")

      object MeetingRecord {
        case object Create extends Permission("Add a tutor meeting record")
        case object Read extends Permission("View a tutor meeting record")
        case object ReadDetails extends Permission("View the contents of a tutor meeting record")
        case object Update extends Permission("Edit a tutor meeting record")
        case object Delete extends Permission("Remove a tutor meeting record")
      }

		}

		// Person's own supervisor ('upward' relationship)
		object Supervisor {
			case object Read extends Permission("View a supervisor")
      object MeetingRecord {
        case object Create extends Permission("Add a supervisor meeting record")
        case object Read extends Permission("View a supervisor meeting record")
        case object ReadDetails extends Permission("View the contents of a supervisor meeting record")
        case object Update extends Permission("Edit a supervisor meeting record")
        case object Delete extends Permission("Remove a supervisor meeting record")
      }
		}
	}

	object SmallGroups {
		case object Archive extends Permission("Archive small groups")

		case object Create extends Permission("Create small groups")
		case object Read extends Permission("View small groups")
		case object Update extends Permission("Edit small groups")
		case object Delete extends Permission("Delete small groups")

		case object Allocate extends Permission("Allocate students to small groups")
		case object AllocateSelf extends Permission("Allocate the current user to a small group")
	}

	object SmallGroupEvents {
		case object Register extends Permission("Record attendance on small group events")
	}

	object UserSettings {
		case object Update extends Permission("Edit user settings")
	}
	
	object MonitoringPoints {
		case object Manage extends Permission("Manage monitoring points")
	}

}
