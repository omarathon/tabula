package uk.ac.warwick.courses.data.convert

import org.springframework.core.convert.converter.Converter
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.model.FeedbackTemplate


class FeedbackTemplateIdConverter extends Converter[String, FeedbackTemplate] with Daoisms {

	override def convert(id: String) = getById[FeedbackTemplate](id).orNull

}
