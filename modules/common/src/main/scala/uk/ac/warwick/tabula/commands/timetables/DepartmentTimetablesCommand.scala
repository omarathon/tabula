package uk.ac.warwick.tabula.commands.timetables

import org.joda.time.{DateTime, Interval, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.{EventOccurrenceList, EventList}
import uk.ac.warwick.tabula.services.timetables.{AutowiringTermBasedEventOccurrenceServiceComponent, EventOccurrenceServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.TimetableEventType
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear, CurrentUser}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Success}

import DepartmentTimetablesCommand._

object DepartmentTimetablesCommand {
	val RequiredPermission = Permissions.Module.ViewTimetable
	val DraftPermission = Permissions.Timetabling.ViewDraft
	val FilterStudentPermission = ViewMemberEventsCommand.RequiredPermission
	val FilterStaffPermission = ViewMemberEventsCommand.RequiredPermission

	private[timetables] type ReturnType = (EventOccurrenceList, Seq[String])
	type CommandType = Appliable[ReturnType]

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
	) with ComposableCommand[ReturnType]
		with AutowiringTermBasedEventOccurrenceServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringSecurityServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringRelationshipServiceComponent
		with ReadOnly with Unaudited
		with DepartmentTimetablesPermissions
		with DepartmentTimetablesCommandState
		with DepartmentTimetablesCommandRequest

	def draft(
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
	) with ComposableCommand[ReturnType]
		with AutowiringTermBasedEventOccurrenceServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringSecurityServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringRelationshipServiceComponent
		with ReadOnly with Unaudited
		with DraftDepartmentTimetablesPermissions
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
)	extends CommandInternal[ReturnType] {
	self: DepartmentTimetablesCommandRequest
		with EventOccurrenceServiceComponent
		with SecurityServiceComponent
		with ModuleAndDepartmentServiceComponent
		with DepartmentTimetablesCommandState =>

	override def applyInternal(): (EventOccurrenceList, Seq[String]) = {
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
		val moduleEvents = EventList.combine(moduleCommands.map { case (module, cmd) =>
			cmd.academicYear = academicYear
			cmd.apply() match {
				case Success(events) =>
					events
				case Failure(t) =>
					errors.append(s"Unable to load timetable for ${module.code.toUpperCase}")
					EventList.empty
			}
		}).map(_.distinct)

		errors.appendAll(students.asScala.filter(student => !studentMembers.exists(_.universityId == student)).map(student =>
			s"Could not find a student with a University ID of $student"
		))
		val studentCommands = studentMembers.map(student => student -> studentPersonalTimetableCommandFactory.apply(student))
		val studentEvents = EventOccurrenceList.combine(studentCommands.map { case (student, cmd) =>
			if (securityService.can(user, FilterStudentPermission, student)) {
				cmd.from = start
				cmd.to = end
				cmd.apply().getOrElse(EventOccurrenceList.empty)
			} else {
				errors.append(s"You do not have permission to view the timetable of ${student.fullName.getOrElse("")} (${student.universityId})")
				EventOccurrenceList.fresh(Nil)
			}
		}).map(_.distinct)

		errors.appendAll(staff.asScala.filter(staffMember => !staffMembers.exists(_.universityId == staffMember)).map(staffMember =>
			s"Could not find a student with a University ID of $staffMember"
		))
		val staffCommands = staffMembers.map(staffMember => staffMember -> staffPersonalTimetableCommandFactory.apply(staffMember))
		val staffEvents = EventOccurrenceList.combine(staffCommands.map { case (staffMember, cmd) =>
			if (securityService.can(user, FilterStaffPermission, staffMember)) {
				cmd.from = start
				cmd.to = end
				cmd.apply().getOrElse(EventOccurrenceList.empty)
			} else {
				errors.append(s"You do not have permission to view the timetable of ${staffMember.fullName.getOrElse("")} (${staffMember.universityId})")
				EventOccurrenceList.fresh(Nil)
			}
		}).map(_.distinct)

		val occurrences = eventsToOccurrences(moduleEvents) ++ studentEvents ++ staffEvents

		val filtered =
			if (eventTypes.asScala.isEmpty) occurrences
			else occurrences.filter { o => eventTypes.contains(o.eventType) }

		// Converter to make localDates sortable
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		(filtered.map(_.sortBy(_.start)), errors.toSeq)
	}

	private def eventsToOccurrences(events: EventList) = EventOccurrenceList(
		events.events.flatMap(eventOccurrenceService.fromTimetableEvent(_, new Interval(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay))),
		events.lastUpdated
	)

}

trait DepartmentTimetablesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DepartmentTimetablesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(RequiredPermission, mandatory(department))
	}
}

trait DraftDepartmentTimetablesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DepartmentTimetablesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(DraftPermission, mandatory(department))
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
	var eventTypes: JList[TimetableEventType] = JArrayList()

	var from: JLong = LocalDate.now.minusMonths(1).toDateTimeAtStartOfDay.getMillis
	var to: JLong = LocalDate.now.plusMonths(1).toDateTimeAtStartOfDay.getMillis
	def start: LocalDate = new DateTime(from * 1000).toLocalDate
	def end: LocalDate = new DateTime(to * 1000).toLocalDate

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

	lazy val personalTutorRelationshipType: StudentRelationshipType = relationshipService.getStudentRelationshipTypeByUrlPart("tutor").getOrElse(
		throw new ItemNotFoundException("Could not find personal tutor relationship type")
	)

	lazy val suggestedStudents: Seq[StudentMember] = (
		studentMembers ++
		user.profile.collect { case m: StudentMember => m }.toSeq ++
		relationshipService.listCurrentRelationshipsWithAgent(personalTutorRelationshipType, user.universityId).flatMap { _.studentMember }
	).distinct.sortBy { m => (m.lastName, m.firstName) }

	lazy val allEventTypes: Seq[TimetableEventType] = {
		val r = TimetableEventType.members
		if (r.contains(null)) {
			logger.error(s"Somehow some of TimetableEventType.members are null. The not-null members are: ${r.filter(_ != null).map(_.code).mkString(", ")}. Skipping the null members...")
		}
		r.filter(_ != null)
	}
}
