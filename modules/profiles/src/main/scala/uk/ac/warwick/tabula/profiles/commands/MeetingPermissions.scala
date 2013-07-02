package uk.ac.warwick.tabula.profiles.commands
import uk.ac.warwick.tabula.permissions.{Permissions, Permission}
import uk.ac.warwick.tabula.data.model.RelationshipType


/**
 * A wrapper around the mappings from the controller actions (create, update, delete, etc) to the
 * relevant tutor/supervisor mp
 */
sealed trait MeetingPermissions {
  val supervisor: Permission
  val tutor: Permission
  def permissionFor(relType:RelationshipType)={
    relType match {
      case RelationshipType.PersonalTutor => tutor
      case RelationshipType.Supervisor => supervisor
    }
  }

}

object MeetingPermissions {

  case object Update extends MeetingPermissions {
    val supervisor = Permissions.Profiles.Supervisor.MeetingRecord.Update
    val tutor = Permissions.Profiles.PersonalTutor.MeetingRecord.Update
  }

  case object Delete extends MeetingPermissions {
    val supervisor = Permissions.Profiles.Supervisor.MeetingRecord.Delete
    val tutor = Permissions.Profiles.PersonalTutor.MeetingRecord.Delete
  }

  case object ReadDetails extends MeetingPermissions {
    val supervisor = Permissions.Profiles.Supervisor.MeetingRecord.ReadDetails
    val tutor = Permissions.Profiles.PersonalTutor.MeetingRecord.ReadDetails
  }

  case object Create extends MeetingPermissions {
    val supervisor = Permissions.Profiles.Supervisor.MeetingRecord.Create
    val tutor = Permissions.Profiles.PersonalTutor.MeetingRecord.Create
  }

  case object Read extends MeetingPermissions {
    val supervisor = Permissions.Profiles.Supervisor.MeetingRecord.Read
    val tutor = Permissions.Profiles.PersonalTutor.MeetingRecord.Read
  }

}