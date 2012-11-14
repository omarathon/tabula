package uk.ac.warwick.tabula.coursework.data.convert

import org.springframework.core.convert.converter.Converter
import uk.ac.warwick.tabula.coursework.data.Daoisms
import uk.ac.warwick.tabula.coursework.data.model.FeedbackTemplate


class FeedbackTemplateIdConverter extends Converter[String, FeedbackTemplate] with Daoisms {

	override def convert(id: String) = getById[FeedbackTemplate](id).orNull

}
