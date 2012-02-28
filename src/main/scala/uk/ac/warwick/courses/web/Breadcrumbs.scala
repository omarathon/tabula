package uk.ac.warwick.courses.web

import uk.ac.warwick.courses.data.model

trait Breadcrumb {
	val title:String
	val url:String
	def linked:Boolean = true
}

object BreadCrumb {
	def apply(title:String, url:String) = Breadcrumbs.Standard(title,url)
}

object Breadcrumbs {
	abstract class Abstract extends Breadcrumb
	case class Standard(val title:String, val url:String) extends Abstract
	
	case class Department(val department:model.Department) extends Abstract {
		val title = department.name
		val url = Routes.admin.department(department)
	}
	
	case class Module(val module:model.Module) extends Abstract {
		val title = module.code.toUpperCase
		val url = Routes.admin.module(module)
	}
	
	case class Current(val title:String) extends Abstract {
		val url = null
		override def linked = false
	}
}