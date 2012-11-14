package uk.ac.warwick.tabula.coursework.web.views

import org.joda.time.DateTime

import freemarker.template.TemplateScalarModel

/**
 * Scalar model you can place into Freemarker. It will return the
 * current year each time it's evaluated.
 */
class TheYear extends TemplateScalarModel {
	def getAsString = new DateTime().getYear.toString
}