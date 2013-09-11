package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Component
import uk.ac.warwick.util.queue.{Queue, QueueListener}
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.tabula.data.Daoisms
import org.codehaus.jackson.annotate.JsonAutoDetect
import scala.beans.BeanProperty
import org.hibernate.stat.Statistics

/**
 * Listens for messages
 */
@Component
class HibernateStatisticsService extends QueueListener with InitializingBean with Logging with Daoisms {

	var queue = Wire.named[Queue]("settingsSyncTopic")
	var context = Wire.property("${module.context}")

	override def isListeningToQueue = true
	override def onReceive(item: Any) {
		logger.info("Updating sessionFactory statistics options " + item + " for " + context)
		item match {
			case HibernateStatisticsMessage("enable") => sessionFactory.getStatistics.setStatisticsEnabled(true)
			case HibernateStatisticsMessage("disable") => sessionFactory.getStatistics.setStatisticsEnabled(false)
			case HibernateStatisticsMessage("clear") => sessionFactory.getStatistics.clear()
			case HibernateStatisticsMessage("log") => logger.info("Hibernate stats for " + context + ": " + getStatisticsSummary(sessionFactory.getStatistics))
		}
	}

	def getStatisticsSummary(stats: Statistics) = {
		s"""
		Flushes: ${stats.getFlushCount}
		Entity loads: ${stats.getEntityLoadCount}
		Entity fetches: ${stats.getEntityFetchCount}
		Collection loads: ${stats.getCollectionLoadCount}

		Query count: ${stats.getQueryExecutionCount}
		Slowest query (${stats.getQueryExecutionMaxTime}ms): ${stats.getQueryExecutionMaxTimeQueryString}

		""".stripMargin
	}

	override def afterPropertiesSet() {
		logger.debug("Registering listener for " + classOf[HibernateStatisticsMessage].getAnnotation(classOf[ItemType]).value + " on " + context)
		queue.addListener(classOf[HibernateStatisticsMessage].getAnnotation(classOf[ItemType]).value, this)
	}

}

@JsonAutoDetect @ItemType("HibernateStatistics")
case class HibernateStatisticsMessage(@BeanProperty var action: String) {
	def this() { this(null) }
}
