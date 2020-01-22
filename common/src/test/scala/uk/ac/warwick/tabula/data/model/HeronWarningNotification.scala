package uk.ac.warwick.tabula.data.model

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.userlookup.User

object HeronWarningNotification {
  val templateLocation = "/WEB-INF/freemarker/notifications/i_really_hate_herons.ftl"
  val batchTemplateLocation = "/WEB-INF/freemarker/notifications/i_really_hate_herons_batch.ftl"
  val heronRant = "They are after your delicious eye jelly. Throw rocks at them!"
}

@Entity
@Proxy
@DiscriminatorValue(value = "HeronWarning")
class HeronWarningNotification extends Notification[MeetingRecord, Unit]
  with SingleItemNotification[MeetingRecord]
  with SingleRecipientNotification
  with MyWarwickActivity
  with BatchedNotification[HeronWarningNotification] {

  import HeronWarningNotification._

  @transient val verb: String = "Heron"

  def title: String = "You all need to know. Herons would love to kill you in your sleep"

  def content = FreemarkerModel(templateLocation, Map("group" -> item, "rant" -> heronRant))

  def url: String = "/beware/herons"

  def urlTitle = "see how evil herons really are"

  def recipient: User = item.entity.relationships.head.agentMember.get.asSsoUser

  override def titleForBatchInternal(notifications: Seq[HeronWarningNotification], user: User): String =
    s"Oh no, ${notifications.size} herons are coming to kill you in your sleep."

  override def contentForBatchInternal(notifications: Seq[HeronWarningNotification]): FreemarkerModel =
    FreemarkerModel(batchTemplateLocation, Map("herons" -> notifications.map(_.item), "rant" -> heronRant))

  override def urlForBatchInternal(notifications: Seq[HeronWarningNotification], user: User): String =
    "/beware/herons/multiple"

  override def urlTitleForBatchInternal(notifications: Seq[HeronWarningNotification]): String =
    "see all your incoming herons"
}

@Entity
@Proxy
@DiscriminatorValue(value = "HeronDefeat")
class HeronDefeatedNotification extends Notification[MeetingRecord, Unit]
  with SingleItemNotification[MeetingRecord] with SingleRecipientNotification
  with MyWarwickActivity {

  import HeronWarningNotification._

  @transient val verb: String = "Heron"

  def title: String = "A heron has been defeated. Rejoice"

  def content = FreemarkerModel(templateLocation, Map("group" -> item, "rant" -> heronRant))

  def url: String = "/beware/herons"

  def urlTitle = "wallow in glory"

  def recipient: User = item.entity.relationships.head.agentMember.get.asSsoUser

}

/*
                                       _____
                                 _.::::::::::::-.
                             _.-:::::='=::=. _   `.
                         _.-'.:'          ""(@)`"- `-----....__
                     _.-'-'.:::....            `' -----------__:=
                  .-'-''.::::::::::._    .       .'""""""""""
                   _.-::::::::::::::')    `-- - :
                .-:::::::::::::'     /  ._.'   )
             .-::::::::::::'    _   .'  .__.:' :
           .::::::::::'      -=' _.'--.  .   ..'
         .:::::::'           _.-'      )  :-'.'
       .::::'  ___..==''          .-   : .  .'
     .::'" _.           _.- _.-        ' .-'
    :'         __.'     _=-'    _.' __' :/
   ,' _.=  _.-==_.  -==        __.. .-'.'
  .'.-_.        _      __..--__..-"_.-''
 .' \ \_.=   -'  __..-'__.--==. .'"
.'\ \_.-= __... __.--'\\     / /
\\__...__.--''"'       \\   ("(
 /_.--'                 \\_.'`.`.
                       .:._.'  `.`.,.     _
                     =''   `:,   `.`.`..-::'
                                   `.` :"
                                   .:.-.`.
                                 .:'    `.:
                                          '
 */
