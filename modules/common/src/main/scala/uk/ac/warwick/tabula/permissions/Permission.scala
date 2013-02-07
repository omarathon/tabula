package uk.ac.warwick.tabula.permissions

sealed trait Permission {
	def getName = Permissions.shortName(getClass.asInstanceOf[Class[_ <: Permission]])
	
	def isScoped = true
}
sealed trait ScopelessPermission extends Permission {
	override def isScoped = false
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
			// Go through the magical heirarchy
			val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
			clz.getConstructors()(0).newInstance().asInstanceOf[Permission]
		} catch {
			case e: ClassNotFoundException => throw new IllegalArgumentException("Permission " + name + " not recognised")
		}
	}
	
	def shortName(clazz: Class[_ <: Permission])
		= clazz.getName.substring(Permissions.getClass.getName.length, clazz.getName.length - 1).replace('$', '.')
	
	/* ScopelessPermissions are Permissions that can be resolved without having to worry about scope */
	case object UserPicker extends ScopelessPermission
	
	case object Masquerade extends ScopelessPermission
	case object GodMode extends ScopelessPermission
	case object ManageMaintenanceMode extends ScopelessPermission
	case object ImportSystemData extends ScopelessPermission
	case object ReplicaSyncing extends ScopelessPermission
	case object PermissionsHelper extends ScopelessPermission
	case object ManageAllDepartmentPermissions extends ScopelessPermission


	object Department {
		case object ManageExtensionSettings extends Permission
		case object ManageDisplaySettings extends Permission
		case object DownloadFeedbackReport extends Permission
		case object ManagePermissions extends Permission
	}
	
	object Module {
		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
		case object ManagePermissions extends Permission
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
	
	object MarkScheme {
		case object Create extends Permission
		case object Read extends Permission
		case object Update extends Permission
		case object Delete extends Permission
	}
	
	object Profiles {
		case object Search extends ScopelessPermission
		case object Read extends Permission
		
		/* We need more fine grained control over what users can see here, so this could be a long list */
		
		object PersonalTutor {
			case object Create extends Permission
			case object Read extends Permission
			case object Update extends Permission
			case object Delete extends Permission
		}
	}
	
	object UserSettings {
		case object Update extends ScopelessPermission
	}
	
}