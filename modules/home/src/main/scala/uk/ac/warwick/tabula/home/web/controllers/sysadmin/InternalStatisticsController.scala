package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.tabula.services._

/**
 * Control the Hibernate SessionFactory's statistics object to log out various
 * statistics on demand, for analysing performance issues.
 */
@Controller
class InternalStatisticsController extends BaseSysadminController with Daoisms {

	// Make all changes through the queue, because each WAR has its own sessionfactory
	var queue = Wire.named[Queue]("settingsSyncTopic")

	@RequestMapping(value=Array("/sysadmin/statistics"))
	def form() = Mav("sysadmin/statistics/form")

	@RequestMapping(value=Array("/sysadmin/statistics/hibernate/toggleAsync.json"), method=Array(POST))
	def toggleHibernateStatistics(@RequestParam enabled: Boolean) = {
		val oldValue = sessionFactory.getStatistics.isStatisticsEnabled()
		queue.send(HibernateStatisticsMessage(if (enabled) "enable" else "disable"))
		new JSONView(Map("enabled" -> enabled, "wasEnabled" -> oldValue))
	}

	@RequestMapping(value=Array("/sysadmin/statistics/hibernate/log"))
	def hibernateStatisticsJson() = {
		queue.send(HibernateStatisticsMessage("log"))
		Mav("sysadmin/statistics/hibernatelogged").noLayout()
	}

	@RequestMapping(value=Array("/sysadmin/statistics/hibernate/clearAsync.json"), method=Array(POST))
	def clearHibernateStatistics() = {
		queue.send(HibernateStatisticsMessage("clear"))
		new JSONView(Map("cleared" -> true))
	}

}
