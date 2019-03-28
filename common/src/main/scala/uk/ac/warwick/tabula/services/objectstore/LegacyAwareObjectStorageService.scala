package uk.ac.warwick.tabula.services.objectstore

import com.google.common.io.ByteSource
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global

import scala.collection.immutable.Seq
import scala.concurrent.Future

class LegacyAwareObjectStorageService(val defaultService: ObjectStorageService, val legacyService: ObjectStorageService) extends ObjectStorageService {

  // Services in order of preference
  private val services = Seq(defaultService, legacyService)

  private def serviceContaining(key: String): Future[Option[ObjectStorageService]] =
    Future.find(services.map { service =>
      service.keyExists(key).map { result =>
        if (result) Some(service) else None
      }
    })(_.nonEmpty).map(_.flatten)

  override def keyExists(key: String): Future[Boolean] = serviceContaining(key).map(_.nonEmpty)

  override def fetch(key: String): Future[RichByteSource] =
    serviceContaining(key).flatMap {
      case Some(service) => service.fetch(key)
      case _ => Future.successful(RichByteSource.empty)
    }

  override def push(key: String, in: ByteSource, metadata: ObjectStorageService.Metadata): Future[Unit] =
    services.head.push(key, in, metadata)

  override def delete(key: String): Future[Unit] =
    Future.sequence(services.map(_.delete(key))).map(_ => ())

  /**
    * Not guaranteed to be distinct (unless you call distinct on it) but shouldn't be used anyway.
    */
  override def listKeys(): Future[Stream[String]] =
    Future.sequence(services.toStream.map(_.listKeys())).map(_.flatten)

  override def afterPropertiesSet(): Unit = services.foreach(_.afterPropertiesSet())
}