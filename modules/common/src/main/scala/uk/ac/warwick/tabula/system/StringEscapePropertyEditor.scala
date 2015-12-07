package uk.ac.warwick.tabula.system

import java.beans.PropertyEditorSupport

import org.apache.commons.lang3.StringEscapeUtils

final class StringEscapePropertyEditor extends PropertyEditorSupport {
	override def setAsText(text: String) {
		super.setValue(escape(text))
	}

	override def getAsText: String = {
		escape(getValue)
	}

	private def escape(textObject: AnyRef): String = {
		textObject match {
			case text: String =>
				StringEscapeUtils.escapeHtml4(text)
			case _ => ""
		}
	}
}