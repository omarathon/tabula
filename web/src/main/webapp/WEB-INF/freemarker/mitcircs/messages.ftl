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

          </div>
          <@bs3form.errors path="message" />
        </@bs3form.form_group>
      </@f.form>
    </div>
  </div>
</#escape>