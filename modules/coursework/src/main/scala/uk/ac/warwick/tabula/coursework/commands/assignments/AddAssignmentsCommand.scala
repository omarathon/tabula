package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import uk.ac.warwick.tabula.commands.{ SelfValidating, Description, Command }
import reflect.BeanProperty
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.data.model.{ Department, UpstreamAssignment }
import uk.ac.warwick.tabula.{ DateFormats, AcademicYear }
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{ Autowired, Configurable }
import uk.ac.warwick.tabula.services.AssignmentService
import org.springframework.validation.Errors
import com.google.common.collect.Maps
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.ValidationUtils
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.ModuleDao
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.beans.factory.annotation.Configurable
import scala.collection.mutable.HashMap
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire


/**
 * Sub-object on the form for binding each upstream assignment and some other properties.
 */
class AssignmentItem(
	// whether to create an assignment from this item or not
	@BeanProperty var include: Boolean,
	@BeanProperty var occurrence: String,
	@BeanProperty var upstreamAssignment: UpstreamAssignment) {
	
    def this() = this(true, null, null)
    
	var assignmentService = Wire.auto[AssignmentService]

	// set after bind
	@BeanProperty var assessmentGroup: Option[UpstreamAssessmentGroup] = _

	// Name for new assignment. Defaults to the name of the upstream assignment, if provided.
	@BeanProperty var name: String = Option(upstreamAssignment).map { _.name }.orNull
	if (upstreamAssignment != null) upstreamAssignment.name else null

	// Will reference a key of AddAssignmentsCommand.optionsMap. In this way, many AssignmentItems
	// can share the same set of options without having to post many copies separately.
	@BeanProperty var optionsId: String = _

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var openDate: DateTime = _

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var closeDate: DateTime = _

}

/**
 * Command for adding many assignments at once, usually from SITS.
 */
class AddAssignmentsCommand(val department: Department) extends Command[Unit] with SelfValidating {

	var assignmentService = Wire.auto[AssignmentService]
	var moduleDao = Wire.auto[ModuleDao]

	// academic year to create all these assignments under. Defaults to whatever academic year it will be in 6
	// months, which means it will start defaulting to next year from about February (under the assumption that
	// you would've done the current year's import long before then).
	@BeanProperty var academicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now.plusMonths(6))

	// All the possible assignments, prepopulated from SITS.
	@BeanProperty var assignmentItems: JList[AssignmentItem] = LazyLists.simpleFactory()

	private def includedItems = assignmentItems.filter { _.include }

	/**
	 * options which are referenced by key by AssignmentItem.optionsId
	 */
	@BeanProperty var optionsMap: JMap[String, SharedAssignmentPropertiesForm] = Maps.newHashMap()

	// just for prepopulating the date form fields.
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty
	val defaultOpenDate = new DateTime().withTime(12, 0, 0, 0)

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty
	val defaultCloseDate = defaultOpenDate.plusWeeks(4)

	override def work() {
		transactional() {
			for (item <- assignmentItems if item.include) {
				val assignment = new Assignment()
				assignment.addDefaultFields()
				assignment.academicYear = academicYear
				assignment.name = item.name
				assignment.upstreamAssignment = item.upstreamAssignment
				assignment.occurrence = item.occurrence
				assignment.module = findModule(item.upstreamAssignment).get

				assignment.openDate = item.openDate
				assignment.closeDate = item.closeDate

				// validation should have verified that there is an options set for us to use
				val options = optionsMap.get(item.optionsId)
				options.copySharedTo(assignment)

				assignmentService.save(assignment)
			}
		}
	}

	def findModule(upstreamAssignment: UpstreamAssignment): Option[Module] = {
		val moduleCode = upstreamAssignment.moduleCodeBasic.toLowerCase
		moduleDao.getByCode(moduleCode)
	}

	override def validate(implicit errors: Errors) {
		ValidationUtils.rejectIfEmpty(errors, "academicYear", "NotEmpty")

		// just get the items we're actually going to import
		val items = includedItems
		val definedOptionsIds = optionsMap.keySet

		def missingOptionId(item: AssignmentItem) = {
			!definedOptionsIds.contains(item.optionsId)
		}

		def missingDates(item: AssignmentItem) = {
			item.openDate == null || item.closeDate == null
		}

		// reject if any items have a missing or garbage optionId value
		if (items.exists(missingOptionId)) {
			errors.reject("assignmentItems.missingOptions")
		}

		// reject if any items are missing date values
		if (items.exists(missingDates)) {
			errors.reject("assignmentItems.missingDates")
		}

		validateNames(errors)

	}

	def validateNames(errors: Errors) {
		val items = includedItems
		val modules = LazyMaps.create { (code: String) => moduleDao.getByCode(code.toLowerCase).orNull }

		for (item <- items) {
			for (existingAssignment <- assignmentService.getAssignmentByNameYearModule(item.name, academicYear, modules(item.upstreamAssignment.moduleCodeBasic))) {
				val path = "assignmentItems[%d]" format (assignmentItems.indexOf(item))
				errors.rejectValue(path, "name.duplicate.assignment", Array(item.name), null)
			}

			def sameNameAs(item: AssignmentItem)(other: AssignmentItem) = {
				other != item && other.name == item.name
			}

			// also check that the upstream assignment names don't collide within a module.
			// group items by module, then look for duplicates within each group.
			val groupedByModule = items.groupBy { _.upstreamAssignment.moduleCodeBasic }
			for ((modCode, moduleItems) <- groupedByModule;
				  item <- moduleItems
				  if moduleItems.exists(sameNameAs(item))) {
				
				val path = "assignmentItems[%d]" format (assignmentItems.indexOf(item))
				// Can't work out why it will end up trying to add the same error multiple times,
				// so wrapping in hasFieldErrors to limit it to showing just the first
				if (!errors.hasFieldErrors(path)) {
				    errors.rejectValue(path, "name.duplicate.assignment.upstream", item.name)
				}
				
			}
		}
	}

	override def describe(description: Description) = {
		description.department(department)
	}

	// do this when first displaying the form. On subsequent POSTs, we should be getting all
	// the info we need from the request.
	def populateWithItems() {
		assignmentItems.clear()
		if (academicYear != null) {
			assignmentItems.addAll(fetchAssignmentItems())
		}
	}

	def afterBind() {
		// re-attach UpstreamAssessmentGroup objects based on the other properties
		for (item <- assignmentItems if item.assessmentGroup == null) {
			item.assessmentGroup = assignmentService.getAssessmentGroup(new UpstreamAssessmentGroup {
				this.academicYear = academicYear
				this.occurrence = item.occurrence
				this.moduleCode = item.upstreamAssignment.moduleCode
				this.assessmentGroup = item.upstreamAssignment.assessmentGroup
			})
		}
	}

	def fetchAssignmentItems(): JList[AssignmentItem] = {
		for {
			upstreamAssignment <- assignmentService.getUpstreamAssignments(department);
			assessmentGroup <- assignmentService.getAssessmentGroups(upstreamAssignment, academicYear).sortBy{ _.occurrence }
		} yield {
			val item = new AssignmentItem(
				include = shouldIncludeByDefault(upstreamAssignment),
				occurrence = assessmentGroup.occurrence,
				upstreamAssignment = upstreamAssignment)
			item.assessmentGroup = Some(assessmentGroup)
			item
		}
	}

	def shouldIncludeByDefault(assignment: UpstreamAssignment) = {
		// currently just exclude "Audit Only" assignments.
		assignment.sequence != "AO"
	}
}
