package uk.ac.warwick.courses.commands.assignments

import collection.JavaConversions._
import uk.ac.warwick.courses.commands.{SelfValidating, Description, Command}
import reflect.BeanProperty
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.helpers.LazyLists
import uk.ac.warwick.courses.data.model.{Department, UpstreamAssignment}
import uk.ac.warwick.courses.AcademicYear
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Configurable}
import uk.ac.warwick.courses.services.AssignmentService
import org.springframework.validation.Errors
import com.google.common.collect.Maps

class SharedAssignmentOptions {

}

class AssignmentItem (
		// whether to create an assignment from this item or not
		@BeanProperty var include: Boolean = true,
		@BeanProperty var upstreamAssignment: UpstreamAssignment = null
	) {

	def this() = this(true)

	// Name for new assignment. Defaults to the name of the upstream assignment, if provided.
	@BeanProperty var name: String = Option(upstreamAssignment).map { _.name }.orNull

	// Will reference the "id" property of an OptionsItem. In this way, many AssignmentItems
	// can share the same set of options without having to post many copies separately.
	@BeanProperty var optionsId: String = _

	@BeanProperty var openDate: DateTime = _
	@BeanProperty var closeDate: DateTime = _

}

/** For binding a set of options */
class OptionsItem (
	@BeanProperty var id: String,
	@BeanProperty var options: ModifyAssignmentCommand
)


/**
 * Command for adding many assignments at once, usually from SITS.
 */
@Configurable
class AddAssignmentsCommand(val department: Department) extends Command[Unit] with SelfValidating {

	@Autowired var assignmentService: AssignmentService = _

	// academic year to create all these assignments under. Defaults to whatever academic year it will be in 6
	// months, which means it will start defaulting to next year from about February (under the assumption that
	// you would've done the current year's import long before then).
	@BeanProperty var academicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now.plusMonths(6))

	// All the possible assignments, prepopulated from SITS.
	@BeanProperty var assignmentItems: JList[AssignmentItem] = LazyLists.simpleFactory()

	@BeanProperty var optionsMap: JMap[String, ModifyAssignmentCommand] = Maps.newHashMap()

	override def apply() {

	}

	override def validate(implicit errors: Errors) {

	}

	override def describe(description: Description) = {
		description.department(department)
	}

	// do this when first displaying the form. On subsequent POSTs, we should be getting all
	// the info we need from the request.
	def populateWithItems() {
		assignmentItems.clear()
		assignmentItems.addAll( fetchAssignmentItems() )
	}


	def fetchAssignmentItems(): JList[AssignmentItem] = {
		for (upstreamAssignment <- assignmentService.getUpstreamAssignments(department)) yield {
			new AssignmentItem(
				include = shouldIncludeByDefault(upstreamAssignment),
				upstreamAssignment = upstreamAssignment
			)
		}
	}

	def shouldIncludeByDefault(assignment: UpstreamAssignment) = {
		// currently just exclude "Audit Only" assignments.
		assignment.sequence != "AO"
	}
}
