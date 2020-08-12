package uk.ac.warwick.tabula.helpers

import org.slf4j.{Logger, LoggerFactory}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Stopwatches._

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

trait Logging {
  @transient val loggerName: String = this.getClass.getName
  @transient lazy val logger: Logger = LoggerFactory.getLogger(loggerName)
  @transient lazy val debugEnabled: Boolean = logger.isDebugEnabled

  @transient val Error: Logging.Level = Logging.Level.Error
  @transient val Warn: Logging.Level = Logging.Level.Warn
  @transient val Info: Logging.Level = Logging.Level.Info
  @transient val Debug: Logging.Level = Logging.Level.Debug

  /**
    * Logs a debug message, with the given arguments inserted into the
    * format placeholders in the message. Checks debugEnabled for you,
    * so no need to do that.
    */
  def debug(message: String, arguments: Any*): Unit =
    if (debugEnabled) logger.debug(message format (arguments: _*))

  /**
    * Log an info message with the size of a collection.
    * Returns the collection so you can wrap it with this without having
    * to break out into a variable.
    */
  def logSize[A](seq: Seq[A], level: Logging.Level = Info)(implicit tag: ClassTag[A]): Seq[A] = {
    log(logger, level, "Collection of " + tag.runtimeClass.getSimpleName + "s: " + seq.size)
    seq
  }

  /**
    * For logging the result of a function without having to break it
    * out into multiple lines.
    */
  def debugResult[A](description: String, result: A): A = {
    debug("%s: %s", description, result)
    result
  }

  /**
    * Wrap some code in a stopwatch, logging some timing info
    * if it takes longer than minMillis.
    */
  def benchmark[A](
    description: String,
    level: Logging.Level = Info,
    minMillis: Int = 0,
    stopWatch: uk.ac.warwick.util.core.StopWatch = StopWatch(),
    logger: Logger = this.logger
  )(fn: => A): A =
    timed(description, level, minMillis, stopWatch, logger) { _ =>
      fn
    }

  /**
    * The same as benchmark, but passes the StopWatch as a callback to the function
    */
  def timed[A](
    description: String,
    level: Logging.Level = Info,
    minMillis: Int = 0,
    stopWatch: uk.ac.warwick.util.core.StopWatch = StopWatch(),
    logger: Logger = this.logger
  )(fn: => uk.ac.warwick.util.core.StopWatch => A): A = {
    if (Logging.benchmarking) {
      try stopWatch.record(description) {
        fn(stopWatch)
      } finally {
        if (stopWatch.getTotalTimeMillis > minMillis) {
          log(logger, level, stopWatch.prettyPrint)
        }
      }
    } else {
      fn(stopWatch)
    }
  }

  /** Do some work, and log the description if an exception occurs.
    * Useful to add context to an exception that occurs within a loop
    * of things.
    */
  protected def tryDescribe[A](desc: => String)(op: => A): A = {
    try {
      if (debugEnabled) logger.debug(desc)
      op
    } catch {
      case e: Exception =>
        logger.error(s"Exception while $desc")
        throw e
    }
  }

  private def log(logger: Logger, level: Logging.Level, message: => String): Unit = level match {
    case Error => if (logger.isErrorEnabled()) logger.error(message)
    case Warn => if (logger.isWarnEnabled()) logger.warn(message)
    case Info => if (logger.isInfoEnabled()) logger.info(message)
    case Debug => if (logger.isDebugEnabled()) logger.debug(message)
  }

}

object Logging {
  var benchmarking = true

  sealed trait Level
  object Level {
    case object Error extends Level
    case object Warn extends Level
    case object Info extends Level
    case object Debug extends Level
  }

  //noinspection ScalaDeprecation
  // We need to convert all Scala collections into Java collections
  // Also handles nulls as "-"
  def convertForStructuredArguments(in: Any): AnyRef = in match {
    case Some(x: Object) => convertForStructuredArguments(x)
    case Some(null) => null
    case None => null
    case jcol: java.util.Collection[_] => jcol.asScala.map(convertForStructuredArguments).asJavaCollection
    case jmap: JMap[_, _] => jmap.asScala.view.mapValues(convertForStructuredArguments).toMap.asJava
    case smap: scala.collection.SortedMap[_, _] => JLinkedHashMap(smap.view.mapValues(convertForStructuredArguments).toSeq: _*)
    case lmap: scala.collection.immutable.ListMap[_, _] => JLinkedHashMap(lmap.view.mapValues(convertForStructuredArguments).toSeq: _*)
    case lmap: scala.collection.mutable.ListMap[_, _] => JLinkedHashMap(lmap.view.mapValues(convertForStructuredArguments).toSeq: _*)
    case smap: scala.collection.Map[_, _] => smap.view.mapValues(convertForStructuredArguments).toMap.asJava
    case smapview: scala.collection.MapView[_, _] => smapview.view.mapValues(convertForStructuredArguments).toMap.asJava
    case sseq: scala.Seq[_] => sseq.map(convertForStructuredArguments).asJava
    case scol: scala.Iterable[_] => scol.map(convertForStructuredArguments).asJavaCollection
    case other: AnyRef => other
    case _ => null
  }
}
