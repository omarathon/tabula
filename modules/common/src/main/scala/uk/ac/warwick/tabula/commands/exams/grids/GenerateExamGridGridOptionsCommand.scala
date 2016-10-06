package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.exams.grids.columns.ExamGridColumnOption
import uk.ac.warwick.tabula.system.permissions.Public

import scala.collection.JavaConverters._

object GenerateExamGridGridOptionsCommand {
	def apply() =
		new GenerateExamGridGridOptionsCommandInternal
			with Command[(Set[ExamGridColumnOption.Identifier], Seq[String])]
			with GenerateExamGridGridOptionsValidation
			with GenerateExamGridGridOptionsCommandState
			with GenerateExamGridGridOptionsCommandRequest
			with ReadOnly with Unaudited with Public
}


class GenerateExamGridGridOptionsCommandInternal extends CommandInternal[(Set[ExamGridColumnOption.Identifier], Seq[String])] {

	self: GenerateExamGridGridOptionsCommandRequest =>

	override def applyInternal() = {
		(predefinedColumnIdentifiers.asScala.toSet, customColumnTitles.asScala)
	}

}

trait GenerateExamGridGridOptionsValidation extends SelfValidating {

	self: GenerateExamGridGridOptionsCommandState with GenerateExamGridGridOptionsCommandRequest =>

	override def validate(errors: Errors) {
		val allIdentifiers = allExamGridsColumns.map(_.identifier).toSet
		val invalidColumns = predefinedColumnIdentifiers.asScala.diff(allIdentifiers)
		if (invalidColumns.nonEmpty) {
			errors.reject("examGrid.invalidColumns", Array(invalidColumns.mkString(", ")), "")
		}
	}

}

trait GenerateExamGridGridOptionsCommandState {

	var allExamGridsColumns: Seq[ExamGridColumnOption] = Wire.all[ExamGridColumnOption]

}

trait GenerateExamGridGridOptionsCommandRequest {

	var predefinedColumnIdentifiers: JSet[String] = JHashSet()
	var yearsToShow: String = "current"
	var customColumnTitles: JList[String] = JArrayList()

}
