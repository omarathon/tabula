package uk.ac.warwick.tabula.web.views

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import org.springframework.validation.Errors
import org.springframework.web.servlet.View
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConverters._
import scala.xml.{Elem, MinimizeMode, Utility}

class XmlErrorView(errors: Errors, filename: Option[String] = None) extends View {
	var messageSource: MessageSource = Wire[MessageSource]

	override def getContentType: String = "text/xml"
	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
		response.setContentType(getContentType())
		response.setStatus(HttpStatus.BAD_REQUEST.value())

		val globalErrors = errors.getGlobalErrors.asScala.map { error => GlobalError(error.getCode, getMessage(error.getCode, error.getArguments: _*)) }.toArray
		val fieldErrors = errors.getFieldErrors.asScala.map { error => FieldError(error.getCode, error.getField, getMessage(error.getCode, error.getArguments: _*)) }.toArray

		val xml: Elem =
			<errors>
				{ (globalErrors ++ fieldErrors).map(_.toXml) }
			</errors>

		filename.foreach(fn => response.setHeader("Content-Disposition", "attachment;filename=\"" + fn + "\""))
		// Scala's main XML library doesn't support XML declarations for some reason. So write one here.
		val writer = response.getWriter
		writer write """<?xml version="1.0" encoding="UTF-8" ?>"""
		writer write "\n"
		writer write Utility.serialize(xml,
			preserveWhitespace = false,
			minimizeTags = MinimizeMode.Always).toString
	}

	def getMessage(key: String, args: Object*): String = messageSource.getMessage(key, if (args == null) Array() else args.toArray, null)

	trait XmlAware {
		def toXml: Elem
	}

	case class GlobalError(code: String, message: String) extends XmlAware {
		def toXml: Elem =
			<error>
				<code>{code}</code>
				<message>{message}</message>
			</error>
	}

	case class FieldError(code: String, field: String, message: String) extends XmlAware {
		def toXml: Elem =
			<error>
				<code>{code}</code>
				<field>{field}</field>
				<message>{message}</message>
			</error>
	}

}
