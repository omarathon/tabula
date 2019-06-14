<#import "*/modal_macros.ftl" as modal />

<#escape x as x?html>
  <div class="message-thread">
    <#if messages?has_content>
      <div class="message-thread__header">
        <h5 class="message-thread__header__title">
          <#if latestMessage??><div class="pull-right"><@fmt.date date=latestMessage /></div></#if>
          <span><@fmt.p number=messages?size singular="message" /></span>
        </h5>
      </div>
      <div class="message-thread__body">
        <#list messages as message>
          <#assign sender>message-thread__body__message--<#if message.studentSent>student<#else>mco</#if></#assign>
          <div class="message-thread__body__message ${sender}">
            <div class="date pull-right"><@fmt.date date=message.createdDate /></div>
            <div class="message-thread__body__message__author">
              <#if studentView>
                <#if message.studentSent>${message.sender.fullName}<#else>Mitigating circumstances officer</#if>
              <#else>
                ${message.sender.fullName}
              </#if>
            </div>
            <div class="message-thread__body__message__content">
              <#noescape>${message.formattedMessage}</#noescape>
              <#if message.attachments?has_content>
                <div class="message-thread__body__message__content__attachments">
                  <ul class="list-unstyled">
                    <#list message.attachments as attachment>
                      <#assign mimeTypeDetectionResult = mimeTypeDetector(attachment) />
                      <li class="attachment">
                        <@fmt.file_type_icon mimeTypeDetectionResult.mediaType />
                        <a href="<@routes.mitcircs.renderMessageAttachment message attachment />" <#if mimeTypeDetectionResult.serveInline>data-inline="true"</#if>>${attachment.name}</a>
                      </li>
                    </#list>
                  </ul>
                </div>
              </#if>
            </div>

            <#if !message_has_next && message.replyByDate??>
              <div class="message-thread__body__message__reply-by-date">
                <i class="fal fa-stopwatch"></i> Respond by <@fmt.date date=message.replyByDate />
              </div>
            </#if>

            <#if !studentView && !message.studentSent>
              <#if message.unreadByStudent>
                <div class="message-thread__body__message__read-indicator message-thread__body__message__read-indicator--unread">
                  <i title="Message sent" class="fal fa-check use-tooltip"></i>
                </div>
              <#else>
                <div class="message-thread__body__message__read-indicator message-thread__body__message__read-indicator--read">
                  <i title="Message read" class="fal fa-check-double use-tooltip"></i>
                </div>
              </#if>
            </#if>
          </div>
        </#list>
      </div>
    </#if>
    <#if submission.canAddMessage>
      <div class="message-thread__footer">
        <@f.form id="mitCircsMessageForm" method="POST" modelAttribute="messageCommand" class="dirty-check double-submit-protection" enctype="multipart/form-data">
          <@bs3form.form_group "message">
            <@bs3form.label path="message">Send a message to <#if studentView>a mitigating circumstances officer<#else>${submission.student.fullName}</#if></@bs3form.label>
            <div class="message-thread__footer__fields">
              <@f.textarea path="message" cssClass="form-control" rows="1" required="true" />

              <#if !studentView>
                <label class="btn btn-primary message-thread__footer__fields__btn">
                  <i class="fal fa-fw fa-paste fa-lg use-tooltip" data-container="body" data-toggle="modal" data-target="#messageTemplates" title="Message templates" aria-label="Message templates"></i>
                </label>

                <div id="messageTemplates" tabindex="-1" class="modal fade message-thread__footer__message-templates">
                  <@modal.wrapper>
                    <@modal.header><h4 class="modal-title">Choose a template</h4></@modal.header>
                    <@modal.body>
                      <dl>
                        <dt><a role="button">Evidence in English</a></dt>
                        <dd>Some of the evidence that you have provided is not in English. For evidence to be considered it must be legible and in English. Evidence obtained overseas which is written in another language must be accompanied by a certified translation.</dd>
                        <dt><a role="button">Impact not described</a></dt>
                        <dd>The evidence that you provide must state how the reported circumstances have impacted on your ability to study and/or complete assessments.</dd>
                      </dl>
                    </@modal.body>
                  </@modal.wrapper>
                </div>
              </#if>

              <label class="btn btn-primary message-thread__footer__fields__btn">
                <input type="file" id="file.upload" name="file.upload" multiple>
                <i class="fal fa-fw fa-paperclip fa-lg use-tooltip" data-container="body" title="Attach files" aria-label="Attach files"></i>
              </label>

              <#if !studentView>
                <label class="btn btn-primary message-thread__footer__fields__btn">
                  <i class="fal fa-fw fa-stopwatch fa-lg use-tooltip" data-container="body" data-toggle="modal" data-target="#replyByDateModal" title="Respond by date" aria-label="Respond by date"></i>
                </label>

                <div id="replyByDateModal" tabindex="-1" class="modal fade message-thread__footer__reply-by-date">
                  <@modal.wrapper>
                    <@modal.header><h4 class="modal-title">Respond by date</h4></@modal.header>
                    <@modal.body>
                      <p>Add a date/time by which ${submission.student.fullName} needs to respond to your message.</p>

                      <p>${submission.student.fullName} will receive a notification 24 hours before this date to remind
                      them to reply to the message, if they haven't already.</p>

                      <p>Sending another message after this one will cancel the reminder.</p>

                      <@bs3form.labelled_form_group labelText="Respond by date" path="replyByDate">
                        <div class="input-group">
                          <@f.input path="replyByDate" autocomplete="off" cssClass="form-control date-time-picker" />
                          <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
                        </div>
                      </@bs3form.labelled_form_group>
                    </@modal.body>
                    <@modal.footer>
                      <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                    </@modal.footer>
                  </@modal.wrapper>
                </div>
              </#if>

              <span class="use-tooltip" data-container="body" title="Send">
                <button type="submit" class="btn btn-primary message-thread__footer__fields__btn" aria-label="Send">
                  <i class="fal fa-fw fa-paper-plane fa-lg"></i>
                </button>
              </span>
            </div>
            <@bs3form.errors path="message" />
          </@bs3form.form_group>
        </@f.form>
      </div>
    </#if>
  </div>
</#escape>