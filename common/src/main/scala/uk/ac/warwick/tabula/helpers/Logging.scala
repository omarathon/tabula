package uk.ac.warwick.tabula.helpers

import org.slf4j.{Logger, LoggerFactory}
import Stopwatches._
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

trait Logging {
  @transient val loggerName: String = this.getClass.getName
  @transient lazy val logger: Logger = LoggerFactory.getLogger(loggerName)
  @transient lazy val debugEnabled: Boolean = logger.isDebugEnabled

  @transient val Error = Logging.Level.Error
  @transient val Warn = Logging.Level.Warn
  @transient val Info = Logging.Level.Info
  @transient val Debug = Logging.Level.Debug

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
    timed(description, level, minMillis, stopWatch, logger) { timer =>
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
  )(fn: => (uk.ac.warwick.util.core.StopWatch => A)): A = {
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
      case e: Exception => {
        logger.error(s"Exception while ${desc}")
        throw e
      }
    }
  }

  private def log(logger: Logger, level: Logging.Level, message: => String) = level match {
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

  // We need to convert all Scala collections into Java collections
  // Also handles nulls as "-"
  def convertForStructuredArguments(in: Any): AnyRef = (in match {
    case Some(x: Object) => convertForStructuredArguments(x)
    case Some(null) => null
    case None => null
    case jcol: java.util.Collection[_] => jcol.asScala.map(convertForStructuredArguments).asJavaCollection
    case jmap: JMap[_, _] => jmap.asScala.mapValues(convertForStructuredArguments).asJava
    case smap: scala.collection.SortedMap[_, _] => JLinkedHashMap(smap.mapValues(convertForStructuredArguments).toSeq: _*)
    case lmap: scala.collection.immutable.ListMap[_, _] => JLinkedHashMap(lmap.mapValues(convertForStructuredArguments).toSeq: _*)
    case lmap: scala.collection.mutable.ListMap[_, _] => JLinkedHashMap(lmap.mapValues(convertForStructuredArguments).toSeq: _*)
    case smap: scala.collection.Map[_, _] => mapAsJavaMapConverter(smap.mapValues(convertForStructuredArguments)).asJava
    case sseq: scala.Seq[_] => seqAsJavaListConverter(sseq.map(convertForStructuredArguments)).asJava
    case scol: scala.Iterable[_] => asJavaCollectionConverter(scol.map(convertForStructuredArguments)).asJavaCollection
    case other: AnyRef => other
    case _ => null
  }) match {
    case null => "-"
    case notNull => notNull
  }
}
