package uk.ac.warwick.tabula.system

import org.springframework.validation.BindingResult

trait BindListener {
	def onBind(result:BindingResult)
}