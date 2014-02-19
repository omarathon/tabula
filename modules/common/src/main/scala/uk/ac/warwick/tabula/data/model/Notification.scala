package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConverters._
import javax.persistence._
import org.hibernate.annotations.Type

import org.joda.time.DateTime

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.services.UserLookupComponent
import org.springframework.util.Assert
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import scala.beans.BeanProperty

object Notification {
	/**
	 * A little explanation...
	 * Without this, every single Notification class needs a constructor that calls a super
	 * init class, which is annoying boilerplate. Since they all have an empty constructor
	 * for Hibernate, this method can be used to fill in the required properties on a new
	 * instance of any Notification.
	 *
	 * e.g.
	 *
	 *    Notification.init(new SubmissionNotification, agent, Seq(submission), assignment)
	 */
	def init[A >: Null <: ToEntityReference, B >: Null <: ToEntityReference, C <: NotificationWithTarget[A, B]]
			(notification: C, agent: User, seq: Seq[A], target: B): C = {
		notification.created = DateTime.now
		notification.agent = agent
		if (target != null) {
			notification.target = target.toEntityReference.asInstanceOf[EntityReference[B]]
		}
		notification.addItems(seq)
		notification
	}

	// factory for single items
	def init[A >: Null <: ToEntityReference, B >: Null <: ToEntityReference, C <: NotificationWithTarget[A, B]]
		(notification: C, agent: User, item: A, target: B): C = init[A,B,C](notification, agent, Seq(item), target)


	def init[A >: Null <: ToEntityReference, C <: Notification[A, Unit]]
			(notification: C, agent: User, seq: Seq[A]): C = {
		notification.created = DateTime.now
		notification.agent = agent
		notification.addItems(seq)
		notification
	}

	// factory for single items
	def init[A >: Null <: ToEntityReference, C <: Notification[A, Unit]]
		(notification: C, agent: User, item: A): C = init[A,C](notification, agent, Seq(item))

}

/**
 * Notifications have a similar structure to Open Social Activities
 * One of the common things we will want to do with notifications is
 * feed them into Open Social activity streams.
 *
 * A notification could be generated when a student submits an assignment
 * in this case ....
 * agent = the student submitting the assignment
 * verb = submit
 * _object = the submission
 * target = the assignment that we are submitting to
 * title = "Submission made"
 * content = "X made a submission to assignment Y on {date_time}"
 * url = "/path/to/assignment/with/this/submission/highlighted"
 * recipients = who is interested in this notification
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="notification_type")
abstract class Notification[A >: Null <: ToEntityReference, B] extends GeneratedId with Serializable with HasSettings {

	@transient final val dateOnlyFormatter = DateFormats.NotificationDateOnly
	@transient final val dateTimeFormatter = DateFormats.NotificationDateTime

	@Column(nullable=false)
	@Type(`type`="uk.ac.warwick.tabula.data.model.SSOUserType")
	final var agent: User = null // the actor in open social activity speak

	@OneToMany(mappedBy="notification", fetch=FetchType.LAZY, targetEntity=classOf[EntityReference[_]], cascade=Array(CascadeType.ALL))
	var items: JList[EntityReference[A]] = JArrayList()

	def entities = items.asScala.map { _.entity }.toSeq

	var created: DateTime = null

	// HasSettings provides the JSONified settings field... ---> HERE <---

	@transient def verb: String
	@transient def title: String
	@transient def content: FreemarkerModel
	@transient def url: String
	@transient def recipients: Seq[User]

	def addItems(seq: Seq[A]) = {
		val x = seq.map { _.toEntityReference }.asInstanceOf[Seq[EntityReference[A]]]
		x.foreach { _.notification = this }
		items.addAll(x.asJava)
		this
	}

	override def toString = List(agent.getFullName, verb, items.getClass.getSimpleName).mkString("notification{", ", ", "}")
}

/**
 * A notification type that has a target must extend this class.
 * We used to have target in Notification but some types don't have a target,
 * and there'd be no valid type for B that could be set to a null EntityReference.
 * So for those types the target parameter is not defined.
 */
@Entity
abstract class NotificationWithTarget[A >: Null <: ToEntityReference, B >: Null <: AnyRef] extends Notification[A,B] {
	@Access(value=AccessType.PROPERTY)
	@OneToOne(cascade = Array(CascadeType.ALL), targetEntity = classOf[EntityReference[B]])
	@BeanProperty
	var target: EntityReference[B] = null
}

object FreemarkerModel {
	trait ContentType
	case object Plain extends ContentType
	case object Html extends ContentType
}
case class FreemarkerModel(template:String, model:Map[String,Any], contentType: FreemarkerModel.ContentType = FreemarkerModel.Plain)

trait SingleItemNotification[A >: Null <: ToEntityReference] {
	self: Notification[A, _] =>

	def item: EntityReference[A] = items.get(0)
}

/** Stores a single recipient as a User ID in the Notification table. */
trait UserIdRecipientNotification extends SingleRecipientNotification with PreSaveBehaviour {

	this : UserLookupComponent =>

	var recipientUserId: String = null
	def recipient = userLookup.getUserByUserId(recipientUserId)

	override def preSave(newRecord: Boolean) {
		Assert.notNull(recipientUserId, "recipientUserId must be set")
	}
}

/** Stores a single recipient as a University ID in the Notification table. */
trait UniversityIdRecipientNotification extends SingleRecipientNotification with PreSaveBehaviour {

	this : UserLookupComponent =>

	var recipientUniversityId: String = null
	def recipient = userLookup.getUserByWarwickUniId(recipientUniversityId)

	override def preSave(newRecord: Boolean) {
		Assert.notNull(recipientUniversityId, "recipientUniversityId must be set")
	}
}

trait SingleRecipientNotification {
	def recipient: User
	def recipients: Seq[User] = {
		Seq(recipient)
	}
}