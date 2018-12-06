package uk.ac.warwick.tabula.exams.grids.columns.studentidentification

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{CourseYearWeighting, StudentCourseYearDetails}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.services.ProgressionService._
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringModuleRegistrationServiceComponent}

@Component
class NameColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "name"

	override val label: String = "Student identification: Name"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.Name

	override val mandatory = true

	private case class FirstNameColumn(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "First Name"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.LongString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(entity.firstName)
			).toMap
		}

	}

	private case class LastNameColumn(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "Last Name"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.LongString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(entity.lastName)
			).toMap
		}

	}

	private case class FullNameColumn(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "Name"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.LongString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(Seq(entity.firstName, entity.lastName).mkString(" "))
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = state.nameToShow match {
		case ExamGridStudentIdentificationColumnValue.FullName => Seq(FullNameColumn(state))
		case ExamGridStudentIdentificationColumnValue.BothName => Seq(FirstNameColumn(state), LastNameColumn(state))
		case _ => Seq()
	}

}

@Component
class UniversityIDColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "universityId"

	override val label: String = "Student identification: University ID"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.UniversityId

	override val mandatory = true

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "ID"
		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map { entity =>
				val scyd = for {
					egeyOpt <- entity.years.get(state.yearOfStudy)
					egey <- egeyOpt
					details <- egey.studentCourseYearDetails
				} yield details
				scyd match {
					case Some(dtls) if !dtls.stale =>
						val componentLink = Routes.Grids.assessmentdetails(dtls)
						entity ->
							ExamGridColumnValueStringWithHtml(
								entity.universityId,
								s"""<a href="$componentLink" target="_blank">${entity.universityId}</a>"""
							)
					case _ => entity -> ExamGridColumnValueString(entity.universityId)
				}
			}.toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

@Component
class SPRCodeColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "sprCode"

	override val label: String = "Student identification: SPR code"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.SPRCode

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "SPR Code"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(
					(for {
						egeyOpt <- entity.years.get(state.yearOfStudy)
						egey <- egeyOpt
						scyd <- egey.studentCourseYearDetails
					} yield scyd.studentCourseDetails.sprCode).getOrElse("[Unknown]")
				)
			).toMap
		}
	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

@Component
class CourseColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "course"

	override val label: String = "Student identification: Course"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.Course

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "Course"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(
					entity.validYears
						.get(state.yearOfStudy)
						.flatMap(_.studentCourseYearDetails)
						.map(scyd => scyd.studentCourseDetails.course.code)
						.getOrElse("[Unknown]")
				)
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

@Component
class RouteColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "route"

	override val label: String = "Student identification: Route"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.Route

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "Route"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(
					entity.validYears.get(state.yearOfStudy).flatMap(_.studentCourseYearDetails).flatMap(
						scyd => Option(scyd.route).map(_.code.toUpperCase)
					).getOrElse("[Unknown]")
				)
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

@Component
class StartYearColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "startyear"

	override val label: String = "Student identification: Start Year"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.StartYear

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "Start Year"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(
					entity.validYears.get(state.yearOfStudy).flatMap(_.studentCourseYearDetails).flatMap(
						scyd => Option(scyd.studentCourseDetails.sprStartAcademicYear)
					).map(_.toString).getOrElse("[Unknown]")
				)
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}


@Component
class YearWeightingsColumnOption extends StudentExamGridColumnOption with AutowiringModuleRegistrationServiceComponent with AutowiringCourseAndRouteServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "yearWeightings"

	override val label: String = "Student identification: Year Weightings"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.YearWeightings

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = "Year Weightings"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map { entity =>
				val studentCourseYearDetails = entity.validYears.get(state.yearOfStudy).flatMap(_.studentCourseYearDetails)
				val weightings = abroadYearWeightings(studentCourseYearDetails)
				val yearWeightingsAsString = for {
					weighting <- weightings
				} yield s"${weighting.weightingAsPercentage.toPlainString}"

				val weightingCol = if (weightings.size > 0) {
					s"${yearWeightingsAsString.mkString("/")} - ${weightings.head.course.code}"
				} else "Year weightings not set"
				entity -> ExamGridColumnValueString(weightingCol)
			}.toMap
		}
	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

	// Find all year weightings. All abroad yrs will have 0 weightings.
	private def abroadYearWeightings(studentCourseYearDetails: Option[StudentCourseYearDetails]): Seq[CourseYearWeighting] = {
		studentCourseYearDetails.map { scyd =>
			val weightings = courseAndRouteService.findAllCourseYearWeightings(Seq(scyd.studentCourseDetails.course), scyd.studentCourseDetails.sprStartAcademicYear)
			val allYearStudentCourseDetails = scyd.studentCourseDetails.student.toExamGridEntity(scyd).years
			weightings.map { yearWeighting =>
				// if any year weightings are non zero they will still be considered 0 if student has gone abroad. We would display 0 if abroad for that course year
				val abroad = allYearStudentCourseDetails.get(yearWeighting.yearOfStudy).exists {
					case Some(ey) => allowEmptyYearMarks(weightings, ey)
					case _ => false
				}
				if (abroad) yearWeighting.copyZeroWeighted else yearWeighting
			}
		}.getOrElse(Seq())
	}
}