package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.{DateTime, Interval, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.timetables.ViewModuleTimetableCommandFactory
import uk.ac.warwick.tabula.data.model.{StaffMember, Department, Module, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.{AutowiringTermBasedEventOccurrenceServiceComponent, EventOccurrenceServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException, PermissionDeniedException}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Success}

object DepartmentTimetablesCommand {
	def apply(
		department: Department,
		user: CurrentUser,
		moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
		studentPersonalTimetableCommandFactory: ViewStudentPersonalTimetableCommandFactory,
		staffPersonalTimetableCommandFactory: ViewStaffPersonalTimetableCommandFactory
	) =	new DepartmentTimetablesCommandInternal(
		department,
		user,
		moduleTimetableCommandFactory,
		studentPersonalTimetableCommandFactory,
		staffPersonalTimetableCommandFactory
	) with ComposableCommand[(Seq[EventOccurrence], Seq[String])]
		with AutowiringTermBasedEventOccurrenceServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringSecurityServiceComponent
		with ReadOnly with Unaudited
		with DepartmentTimetablesPermissions
		with DepartmentTimetablesCommandState
		with DepartmentTimetablesCommandRequest

}


class DepartmentTimetablesCommandInternal(
	val department: Department,
	user: CurrentUser,
	moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
	studentPersonalTimetableCommandFactory: ViewStudentPersonalTimetableCommandFactory,
	staffPersonalTimetableCommandFactory: ViewStaffPersonalTimetableCommandFactory
)	extends CommandInternal[(Seq[EventOccurrence], Seq[String])] {

	self: DepartmentTimetablesCommandRequest with EventOccurrenceServiceComponent with SecurityServiceComponent =>

	override def applyInternal() = {
		val errors: mutable.Buffer[String] = mutable.Buffer()
		val moduleCommands = modules.asScala.map(module => module -> moduleTimetableCommandFactory.apply(module))
		val moduleEvents = moduleCommands.flatMap{case(module, cmd) => cmd.apply() match {
			case Success(events) =>
				events
			case Failure(t) =>
				errors.append(s"Unable to load timetable for ${module.code.toUpperCase}")
				Seq()
		}}.distinct

		errors.appendAll(students.asScala.filter(student => studentMembers.find(_.universityId == student).isEmpty).map(student =>
			s"Could not find a student with a University ID of $student"
		))
		val studentCommands = studentMembers.map(student => student -> studentPersonalTimetableCommandFactory.apply(student))
		val studentEvents = studentCommands.flatMap{case(student, cmd) =>
			try {
				permittedByChecks(securityService, user, cmd)
				Some(cmd.apply())
			} catch {
				case e @ (_ : ItemNotFoundException | _ : PermissionDeniedException) =>
					errors.append(s"You do not have permission to view the timetable of ${student.fullName.getOrElse("")} (${student.universityId})")
					None
			}
		}.flatten.distinct

		errors.appendAll(staff.asScala.filter(staffMember => staffMembers.find(_.universityId == staffMember).isEmpty).map(staffMember =>
			s"Could not find a student with a University ID of $staffMember"
		))
		val staffCommands = staffMembers.map(staffMember => staffMember -> staffPersonalTimetableCommandFactory.apply(staffMember))
		val staffEvents = staffCommands.flatMap{case(staffMember, cmd) =>
			try {
				permittedByChecks(securityService, user, cmd)
				Some(cmd.apply())
			} catch {
				case e @ (_ : ItemNotFoundException | _ : PermissionDeniedException) =>
					errors.append(s"You do not have permission to view the timetable of ${staffMember.fullName.getOrElse("")} (${staffMember.universityId})")
					None
			}
		}.flatten.distinct

		val occurrences = moduleEvents.flatMap(eventsToOccurrences) ++ studentEvents ++ staffEvents

		// Converter to make localDates sortable
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		(occurrences.sortBy(_.start), errors.toSeq)
	}

	private def eventsToOccurrences: TimetableEvent => Seq[EventOccurrence] =
		eventOccurrenceService.fromTimetableEvent(_, new Interval(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay))

}

trait DepartmentTimetablesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DepartmentTimetablesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Search, department)
	}

}

trait DepartmentTimetablesCommandState {
	def department: Department
}

trait DepartmentTimetablesCommandRequest extends PermissionsCheckingMethods {

	self: DepartmentTimetablesCommandState with ProfileServiceComponent =>

	var modules: JList[Module] = JArrayList()
	var students: JList[String] = JArrayList()
	lazy val studentMembers: Seq[StudentMember] = profileService.getAllMembersWithUniversityIds(students.asScala).flatMap{
		case student: StudentMember => Option(student)
		case _ => None
	}
	var staff: JList[String] = JArrayList()
	lazy val staffMembers: Seq[StaffMember] = profileService.getAllMembersWithUniversityIds(staff.asScala).flatMap{
		case staffMember: StaffMember => Option(staffMember)
		case _ => None
	}

	var from: JLong = LocalDate.now.minusMonths(1).toDateTimeAtStartOfDay.getMillis
	var to: JLong = LocalDate.now.plusMonths(1).toDateTimeAtStartOfDay.getMillis
	def start = new DateTime(from * 1000).toLocalDate
	def end = new DateTime(to * 1000).toLocalDate

	private def modulesForDepartmentAndSubDepartments(department: Department): Seq[Module] =
		(department.modules.asScala ++ department.children.asScala.flatMap { modulesForDepartmentAndSubDepartments }).sorted

	lazy val allModules: Seq[Module] = ((modulesForDepartmentAndSubDepartments(mandatory(department)) match {
		case Nil => modulesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
		case someModules => someModules
	}) ++ modules.asScala).distinct.sorted
}
