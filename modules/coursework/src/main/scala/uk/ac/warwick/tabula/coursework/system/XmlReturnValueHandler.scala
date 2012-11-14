package uk.ac.warwick.tabula.coursework.system

import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import scala.xml.Elem
import org.springframework.core.MethodParameter
import org.springframework.web.method.support.ModelAndViewContainer
import uk.ac.warwick.tabula.coursework.web.Mav
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.servlet.View
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.JavaImports._
import xml.Utility

class XmlReturnValueHandler extends HandlerMethodReturnValueHandler {

	override def supportsReturnType(methodParam: MethodParameter) = {
		classOf[Elem] isAssignableFrom methodParam.getMethod.getReturnType
	}

	override def handleReturnValue(returnValue: Object,
		returnType: MethodParameter,
		mavContainer: ModelAndViewContainer,
		webRequest: NativeWebRequest) =
		returnValue match {
			case xml: Elem => mavContainer.setView(new XmlView(xml))
		}

	/** Renders an XML element as a document. Adds XML decl and sets text/xml content type. */
	class XmlView(xml: Elem) extends View {
		def getContentType: String = "text/xml"
		def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse) {
			// Scala's main XML library doesn't support XML declarations for some reason. So write one here.
			val writer = response.getWriter
			writer write """<?xml version="1.0" encoding="UTF-8" ?>"""
			writer write ("\n")
			writer write Utility.toXML(xml,
				preserveWhitespace = false,
				minimizeTags = true).toString
		}
	}
}
