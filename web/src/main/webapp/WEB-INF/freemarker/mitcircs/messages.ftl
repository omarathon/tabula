<#import "*/modal_macros.ftl" as modal />

<#escape x as x?html>
  <div class="message-thread">
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
          <div class="message-thread__body__message__author">${message.sender.fullName}</div>
          <div class="message-thread__body__message__content">
            <#noescape>${message.formattedMessage}</#noescape>
            <#if message.attachments?has_content>
              <div class="message-thread__body__message__content__attachments">
                <ul class="list-unstyled">
                  <#list message.attachments as attachment>
                    <li>
                      <i class="fal fa-paperclip"></i>
                      <a href="">${attachment.name}</a>
                    </li>
                  </#list>
                </ul>
              </div>
            </#if>
          </div>
        </div>
      </#list>
    </div>
    <div class="message-thread__footer">
      <@f.form id="mitCircsMessageForm" method="POST" modelAttribute="messageCommand" class="dirty-check double-submit-protection" enctype="multipart/form-data">
        <@bs3form.form_group "message">
          <@bs3form.label path="message">Send a message</@bs3form.label>
          <div class="message-thread__footer__fields">
            <@f.textarea path="message" cssClass="form-control" rows="1" required="true" />

            <span class="use-tooltip" title="Send" data-original-title="Send">
              <button type="submit" class="btn btn-primary" aria-label="Send">
                <i class="fal fa-fw fa-paper-plane fa-lg"></i>
              </button>
            </span>

            <label class="btn btn-primary">
              <input type="file" id="file.upload" name="file.upload" multiple>
              <i class="fal fa-fw fa-paperclip fa-lg use-tooltip" title="Attach files" data-original-title="Attach files"></i>
            </label>

            <label class="btn btn-primary">
              <i class="fal fa-fw fa-paste fa-lg use-tooltip" data-toggle="modal" data-target="#messageTemplates" title="Message templates"></i>
            </label>
          </div>
          <@bs3form.errors path="message" />
        </@bs3form.form_group>
      </@f.form>
      <div id="messageTemplates" class="modal fade message-thread__footer__message-templates">
        <@modal.wrapper>
          <@modal.header><h6 class="modal-title">Choose a template</h6></@modal.header>
          <@modal.body>
            <dl>
              <dt><a role="button" tabindex="0">Evidence in English</a></dt>
              <dd>Some of the evidence that you have provided is not in English. For evidence to be considered it must be legible and in English. Evidence obtained overseas which is written in another language must be accompanied by a certified translation.</dd>
              <dt><a role="button" tabindex="0">Impact not described</a></dt>
              <dd>The evidence that you provide must state how the reported circumstances have impacted on your ability to study and/or complete assessments.</dd>
            </dl>
          </@modal.body>
        </@modal.wrapper>
      </div>
    </div>
  </div>
</#escape>