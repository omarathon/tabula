package uk.ac.warwick.tabula.permissions

sealed abstract class Permission
sealed abstract class ScopelessPermission extends Permission

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
			val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$'))
			clz.getConstructors()(0).newInstance().asInstanceOf[Permission]
		} catch {
			case e: ClassNotFoundException => throw new IllegalArgumentException("Permission " + name + " not recognised")
		}
	}
	
	/* ScopelessPermissions are Permissions that can be resolved without having to worry about scope */
	case class UserPicker() extends ScopelessPermission
	
	case class Masquerade() extends ScopelessPermission
	case class GodMode() extends ScopelessPermission
	case class ManageMaintenanceMode() extends ScopelessPermission
	case class ImportSystemData() extends ScopelessPermission
	case class ReplicaSyncing() extends ScopelessPermission
	
	object Department {
		case class ManageExtensionSettings() extends Permission
		case class ManageDisplaySettings() extends Permission
		case class DownloadFeedbackReport() extends Permission
		case class ManagePermissions() extends Permission
	}
	
	object Module {
		case class Create() extends Permission
		case class Read() extends Permission
		case class Update() extends Permission
		case class Delete() extends Permission
		case class ManagePermissions() extends Permission
	}
			
	object Assignment {
		case class ImportFromExternalSystem() extends Permission
		case class Archive() extends Permission
		
		case class Create() extends Permission
		case class Read() extends Permission
		case class Update() extends Permission
		case class Delete() extends Permission
	}
				
	object Submission {
		case class ViewPlagiarismStatus() extends Permission
		case class ManagePlagiarismStatus() extends Permission
		case class CheckForPlagiarism() extends Permission
		case class SendReceipt() extends Permission
		case class ReleaseForMarking() extends Permission
		
		case class Create() extends Permission
		case class Read() extends Permission
		case class Update() extends Permission
		case class Delete() extends Permission
	}
	
	object Feedback {
		case class Publish() extends Permission
		case class Rate() extends Permission
		
		case class Create() extends Permission
		case class Read() extends Permission
		case class Update() extends Permission
		case class Delete() extends Permission
	}
	
	object Marks {
		case class DownloadTemplate() extends Permission
		
		case class Create() extends Permission
		case class Read() extends Permission
		case class Update() extends Permission
		case class Delete() extends Permission
	}
				
	object Extension {
		case class MakeRequest() extends Permission
		case class ReviewRequest() extends Permission
		
		case class Create() extends Permission
		case class Read() extends Permission
		case class Update() extends Permission
		case class Delete() extends Permission
	}
				
	object FeedbackTemplate {
		case class Create() extends Permission
		case class Read() extends Permission
		case class Update() extends Permission
		case class Delete() extends Permission
	}
	
	object MarkScheme {
		case class Create() extends Permission
		case class Read() extends Permission
		case class Update() extends Permission
		case class Delete() extends Permission
	}
	
	object Profiles {
		case class Search() extends ScopelessPermission
		case class Read() extends Permission
		
		/* We need more fine grained control over what users can see here, so this could be a long list */
		
		object PersonalTutor {
			case class Create() extends Permission
			case class Read() extends Permission
			case class Update() extends Permission
			case class Delete() extends Permission			
		}
	}
}