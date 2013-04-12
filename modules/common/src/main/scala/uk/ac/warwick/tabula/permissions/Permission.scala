package uk.ac.warwick.tabula.permissions

import uk.ac.warwick.tabula.CaseObjectEqualityFixes

sealed trait Permission extends CaseObjectEqualityFixes[Permission] {
	val getName = Permissions.shortName(getClass.asInstanceOf[Class[_ <: Permission]])

	val isScoped = true
}
sealed trait ScopelessPermission extends Permission {
	override val isScoped = false
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
	case object UserPicker extends ScopelessPermission

	case object Masquerade extends ScopelessPermission
	case object GodMode extends ScopelessPermission
	case object ManageMaintenanceMode extends ScopelessPermission
	case object ImportSystemData extends ScopelessPermission
	case object ReplicaSyncing extends ScopelessPermission

	object RolesAndPermissions {
		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}

	object Department {
		case object ManageExtensionSettings extends Permission
		case object ManageDisplaySettings extends Permission
		case object DownloadFeedbackReport extends Permission
	}

	object Module {
		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}

	object Assignment {
		case object ImportFromExternalSystem extends Permission
		case object Archive extends Permission

		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}

	object Submission {
		case object ViewPlagiarismStatus extends Permission
		case object ManagePlagiarismStatus extends Permission
		case object CheckForPlagiarism extends Permission
		case object SendReceipt extends Permission
		case object ReleaseForMarking extends Permission

		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}

	object Feedback {
		case object Publish extends Permission
		case object Rate extends Permission

		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}

	object Marks {
		case object DownloadTemplate extends Permission

		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}

	object Extension {
		case object MakeRequest extends Permission
		case object ReviewRequest extends Permission

		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}

	object FeedbackTemplate {
		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}

	object MarkingWorkflow {
		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}

	object Profiles {
		case object Search extends ScopelessPermission

		object Read {
			case object Core extends Permission // Photo, name, course, Warwick email, job title
			case object UniversityId extends Permission // University number
			case object DateOfBirth extends Permission
			case object Nationality extends Permission
			case object Gender extends Permission
			case object NextOfKin extends Permission
			case object HomeAddress extends Permission
			case object TermTimeAddress extends Permission
			case object TelephoneNumber extends Permission
			case object MobileNumber extends Permission
			case object HomeEmail extends Permission
			case object Usercode extends Permission
			case object PersonalTutees extends Permission // Person's tutees ('downward' relationship)
			case object StudyDetails extends Permission
		}

		// Person's own tutor ('upward' relationship)
		object PersonalTutor {
			case object Upload extends Permission

			case object Create extends Permission
			case object Read extends Permission
			case object Update extends Permission
			case object Delete extends Permission
		}

		object MeetingRecord {
			case object Create extends Permission
			case object Read extends Permission
			case object Update extends Permission
			case object Delete extends Permission
		}
	}

	object UserSettings {
		case object Update extends Permission
	}

}