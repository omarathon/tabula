package uk.ac.warwick.tabula.permissions

import uk.ac.warwick.tabula.CaseObjectEqualityFixes
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.EqualsBuilder
import scala.reflect.ClassTag

sealed abstract class Permission(val description: String) extends CaseObjectEqualityFixes[Permission] {
	val getName = Permissions.shortName(getClass.asInstanceOf[Class[_ <: Permission]])

	val isScoped = true
}
sealed abstract class ScopelessPermission(description: String) extends Permission(description) {
	override val isScoped = false
}
sealed abstract class SelectorPermission[A <: PermissionsSelector[A]](val selector: PermissionsSelector[A], description: String) extends Permission(description) {
	override val getName = SelectorPermission.shortName(getClass.asInstanceOf[Class[_ <: SelectorPermission[A]]])
	def <= [B <: PermissionsSelector[B]](other: SelectorPermission[B]) = other match {
		case that: SelectorPermission[A] => selector <= that.selector.asInstanceOf[PermissionsSelector[A]]
		case _ => false
	}
	
	override def equals(other: Any) = other match {
		case that: SelectorPermission[A] => {
			new EqualsBuilder()
			.append(getName, that.getName)
			.append(selector, that.selector)
			.build()
		}
		case _ => false
	}
	
	override def hashCode() = 
		new HashCodeBuilder()
		.append(getName)
		.append(selector)
		.build()
		
	override def toString() = "%s(%s)".format(super.toString, selector) 
}

trait PermissionsSelector[A <: PermissionsSelector[A]] {
	def id: String
	def description:String
	def isWildcard = false
	def <=(that: PermissionsSelector[A]) = that match {
		case any if any.isWildcard => true
		case that => this == that
	}
}

object PermissionsSelector {
	val AnyId = "*" // A special ID for converting to and from the catch-all selector

	def Any[A <: PermissionsSelector[A] : ClassTag] = new PermissionsSelector[A] {
		def id = AnyId
		def description = "*"
		override def isWildcard = true

		override def <=(that: PermissionsSelector[A]) = {
			// Any is only <= other wildcards
			that.isWildcard
		}

		override def toString() = "*"

		override def hashCode = id.hashCode

		override def equals(other: Any) = other match {
			case that: PermissionsSelector[A] => {
				new EqualsBuilder()
					.append(id, that.id)
					.build()
			}
			case _ => false
		}

	}
}

case class CheckablePermission(val permission: Permission, val scope: Option[PermissionsTarget])

object CheckablePermission {
	def apply(permission: ScopelessPermission): CheckablePermission = new CheckablePermission(permission, None)
	def apply(permission: Permission, scope: PermissionsTarget): CheckablePermission = new CheckablePermission(permission, Some(scope))
}

object SelectorPermission {
	private val ObjectClassPrefix = Permissions.getClass.getName
	
	def of[A <: PermissionsSelector[A]](name: String, selector: A): SelectorPermission[A] = {
		try {
			// Go through the magical hierarchy
			val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$'))
			clz.getConstructors()(0).newInstance(selector).asInstanceOf[SelectorPermission[A]]
		} catch {
			case e: ClassNotFoundException => throw new IllegalArgumentException("Selector permission " + name + " not recognised")
		}
	}
	
	def shortName(clazz: Class[_ <: SelectorPermission[_]])
		= clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length).replace('$', '.')
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
		case object ArrangeRoutes extends Permission("Sort routes into sub-departments")
		case object ManageExtensionSettings extends Permission("Manage extension settings")
		case object ManageDisplaySettings extends Permission("Manage display settings")
		case object DownloadFeedbackReport extends Permission("Generate a feedback report")
		case object ManageProfiles extends Permission("Manage student profiles")
		case object Create extends Permission("Add a sub-department")
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
			case object NextOfKin extends Permission("View a member's next of kin")
			case object HomeAddress extends Permission("View a member's home address")
			case object TermTimeAddress extends Permission("View a member's term-time address")
			case object TelephoneNumber extends Permission("View a member's telephone number")
			case object MobileNumber extends Permission("View a member's mobile number")
			case object HomeEmail extends Permission("View a member's alternative email address")
			case object Usercode extends Permission("View a member's usercode")
			case object SmallGroups extends Permission("View a member's small groups")
			case object Timetable extends Permission("View a member's personal timetable")

			object StudentCourseDetails {
				case object Core extends Permission("View a student's basic course, route and department details")
				case object Status extends Permission("View a student's enrolment and study status")
			}
			
			case class RelationshipStudents(relationshipType: PermissionsSelector[StudentRelationshipType]) 
				extends SelectorPermission(relationshipType, "View a member's students")
		}
		
		object StudentRelationship {		
			case class Create(relationshipType: PermissionsSelector[StudentRelationshipType]) 
				extends SelectorPermission(relationshipType, "Add a student relationship")
			case class Read(relationshipType: PermissionsSelector[StudentRelationshipType]) 
				extends SelectorPermission(relationshipType, "View a student relationship")
			case class Update(relationshipType: PermissionsSelector[StudentRelationshipType]) 
				extends SelectorPermission(relationshipType, "Edit a student relationship")
			case class Delete(relationshipType: PermissionsSelector[StudentRelationshipType]) 
				extends SelectorPermission(relationshipType, "Remove a student relationship")
		}
		
		object MeetingRecord {
      case class Create(relationshipType: PermissionsSelector[StudentRelationshipType]) 
      	extends SelectorPermission(relationshipType, "Add a meeting record")
      case class Read(relationshipType: PermissionsSelector[StudentRelationshipType]) 
      	extends SelectorPermission(relationshipType, "View a meeting record")
      case class ReadDetails(relationshipType: PermissionsSelector[StudentRelationshipType]) 
      	extends SelectorPermission(relationshipType, "View the contents of a meeting record")
      case class Update(relationshipType: PermissionsSelector[StudentRelationshipType]) 
      	extends SelectorPermission(relationshipType, "Edit a meeting record")
      case class Delete(relationshipType: PermissionsSelector[StudentRelationshipType]) 
      	extends SelectorPermission(relationshipType, "Remove a meeting record")
    }
	}

	object SmallGroups {
		case object Archive extends Permission("Archive small groups")

		case object Create extends Permission("Create small groups")
		case object Read extends Permission("View small groups")
		case object ReadMembership extends Permission("View small group membership")
		case object Update extends Permission("Edit small groups")
		case object Delete extends Permission("Delete small groups")

		case object Allocate extends Permission("Allocate students to small groups")
		case object AllocateSelf extends Permission("Allocate the current user to a small group")
	}

	object SmallGroupEvents {
		case object Register extends Permission("Record attendance on small group events")
		case object ViewRegister extends Permission("View attendance at small group events")
	}

	object UserSettings {
		case object Update extends Permission("Edit user settings")
	}
	
	object MonitoringPoints {
		case object Manage extends Permission("Manage monitoring points")
		case object View extends Permission("View monitoring points")
		case object Record extends Permission("Record monitoring points")
		case object Report extends Permission("Report monitoring points")
	}

	object MonitoringPointSetTemplates {
		case object View extends ScopelessPermission("View monitoring point set templates")
		case object Manage extends ScopelessPermission("Manage monitoring point set templates")
	}
	
	object StudentRelationshipType {
		case object Create extends ScopelessPermission("Create student relationship types")
		case object Read extends ScopelessPermission("View student relationship types")
		case object Update extends ScopelessPermission("Edit student relationship types")
		case object Delete extends ScopelessPermission("Remove student relationship types")
	}

	object MemberNotes {
		case object Read extends Permission("View member notes")
		case object ReadMetadata extends Permission("View metadata on member notes")
		case object Create extends Permission("Create member notes")
		case object Update extends Permission("Edit member notes")
		case object Delete extends Permission("Delete member notes")
	}
}
