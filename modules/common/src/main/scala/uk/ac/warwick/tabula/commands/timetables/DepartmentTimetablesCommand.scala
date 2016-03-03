package uk.ac.warwick.tabula.commands.timetables

import org.joda.time.{DateTime, Interval, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.{AutowiringTermBasedEventOccurrenceServiceComponent, EventOccurrenceServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear, CurrentUser}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Success}

import DepartmentTimetablesCommand._

object DepartmentTimetablesCommand {
	val RequiredPermission = Permissions.Module.ViewTimetable
	val FilterStudentPermission = ViewMemberEventsCommand.RequiredPermission
	val FilterStaffPermission = ViewMemberEventsCommand.RequiredPermission

	def apply(
		department: Department,
		academicYear: AcademicYear,
		user: CurrentUser,
		moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
		studentPersonalTimetableCommandFactory: ViewStudentPersonalTimetableCommandFactory,
		staffPersonalTimetableCommandFactory: ViewStaffPersonalTimetableCommandFactory
	) =	new DepartmentTimetablesCommandInternal(
		department,
		user,
		academicYear,
		moduleTimetableCommandFactory,
		studentPersonalTimetableCommandFactory,
		staffPersonalTimetableCommandFactory
	) with ComposableCommand[(Seq[EventOccurrence], Seq[String])]
		with AutowiringTermBasedEventOccurrenceServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringSecurityServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringRelationshipServiceComponent
		with ReadOnly with Unaudited
		with DepartmentTimetablesPermissions
		with DepartmentTimetablesCommandState
		with DepartmentTimetablesCommandRequest

}

class DepartmentTimetablesCommandInternal(
	val department: Department,
	val user: CurrentUser,
	academicYear: AcademicYear,
	moduleTimetableCommandFactory: ViewModuleTimetableCommandFactory,
	studentPersonalTimetableCommandFactory: ViewStudentPersonalTimetableCommandFactory,
	staffPersonalTimetableCommandFactory: ViewStaffPersonalTimetableCommandFactory
)	extends CommandInternal[(Seq[EventOccurrence], Seq[String])] {
	self: DepartmentTimetablesCommandRequest
		with EventOccurrenceServiceComponent
		with SecurityServiceComponent
		with ModuleAndDepartmentServiceComponent
		with DepartmentTimetablesCommandState =>

	override def applyInternal() = {
		val errors: mutable.Buffer[String] = mutable.Buffer()

		val routesModules = moduleAndDepartmentService.findModulesByRoutes(routes.asScala, academicYear)
		val yearOfStudyModules = moduleAndDepartmentService.findModulesByYearOfStudy(department, yearsOfStudy.asScala, academicYear)

		val queryModules = (
			modules.asScala ++ routesModules ++ yearOfStudyModules
		).distinct.filter { module =>
			(modules.isEmpty || modules.contains(module)) &&
			(routes.isEmpty || routesModules.contains(module)) &&
			(yearsOfStudy.isEmpty || yearOfStudyModules.contains(module))
		}

		val moduleCommands = queryModules.map(module => module -> moduleTimetableCommandFactory.apply(module))
		val moduleEvents = moduleCommands.flatMap { case(module, cmd) => cmd.apply() match {
			case Success(events) =>
				events
			case Failure(t) =>
				errors.append(s"Unable to load timetable for ${module.code.toUpperCase}")
				Seq()
		}}.distinct

		errors.appendAll(students.asScala.filter(student => !studentMembers.exists(_.universityId == student)).map(student =>
			s"Could not find a student with a University ID of $student"
		))
		val studentCommands = studentMembers.map(student => student -> studentPersonalTimetableCommandFactory.apply(student))
		val studentEvents = studentCommands.flatMap { case (student, cmd) =>
			if (securityService.can(user, FilterStudentPermission, student)) {
				cmd.from = start
				cmd.to = end
				cmd.apply().toOption
			} else {
				errors.append(s"You do not have permission to view the timetable of ${student.fullName.getOrElse("")} (${student.universityId})")
				None
			}
		}.flatten.distinct

		errors.appendAll(staff.asScala.filter(staffMember => !staffMembers.exists(_.universityId == staffMember)).map(staffMember =>
			s"Could not find a student with a University ID of $staffMember"
		))
		val staffCommands = staffMembers.map(staffMember => staffMember -> staffPersonalTimetableCommandFactory.apply(staffMember))
		val staffEvents = staffCommands.flatMap { case (staffMember, cmd) =>
			if (securityService.can(user, FilterStaffPermission, staffMember)) {
				cmd.from = start
				cmd.to = end
				cmd.apply().toOption
			} else {
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
		p.PermissionCheck(RequiredPermission, mandatory(department))
	}
}

trait DepartmentTimetablesCommandState {
	def user: CurrentUser
	def department: Department
}

trait DepartmentTimetablesCommandRequest extends PermissionsCheckingMethods {
	self: DepartmentTimetablesCommandState with ProfileServiceComponent with RelationshipServiceComponent =>

	var modules: JList[Module] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var students: JList[String] = JArrayList()
	lazy val studentMembers: Seq[StudentMember] = profileService.getAllMembersWithUniversityIds(students.asScala).flatMap {
		case student: StudentMember => Option(student)
		case _ => None
	}
	var staff: JList[String] = JArrayList()
	lazy val staffMembers: Seq[StaffMember] = profileService.getAllMembersWithUniversityIds(staff.asScala).flatMap {
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

	private def routesForDepartmentAndSubDepartments(department: Department): Seq[Route] =
		(department.routes.asScala ++ department.children.asScala.flatMap { routesForDepartmentAndSubDepartments }).sorted

	lazy val allRoutes: Seq[Route] = ((routesForDepartmentAndSubDepartments(mandatory(department)) match {
		case Nil => routesForDepartmentAndSubDepartments(mandatory(department.rootDepartment))
		case someRoutes => someRoutes
	}) ++ routes.asScala).distinct.sorted(Route.DegreeTypeOrdering)

	lazy val allYearsOfStudy: Seq[Int] = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy

	lazy val suggestedStaff: Seq[StaffMember] = (
		staffMembers ++
		user.profile.collect { case m: StaffMember => m }.toSeq
	).distinct.sortBy { m => (m.lastName, m.firstName) }

	lazy val personalTutorRelationshipType = relationshipService.getStudentRelationshipTypeByUrlPart("tutor").getOrElse(
		throw new ItemNotFoundException("Could not find personal tutor relationship type")
	)

	lazy val suggestedStudents: Seq[StudentMember] = (
		studentMembers ++
		user.profile.collect { case m: StudentMember => m }.toSeq ++
		relationshipService.listCurrentRelationshipsWithAgent(personalTutorRelationshipType, user.universityId).flatMap { _.studentMember }
	).distinct.sortBy { m => (m.lastName, m.firstName) }
}
