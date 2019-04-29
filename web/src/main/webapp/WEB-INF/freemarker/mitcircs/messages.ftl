<#escape x as x?html>
  <div class="message-thread">
    <#list messages as message>
      <#assign sender><#if message.studentSent>message-thread__message--student<#else>message-thread__message--mco</#if></#assign>
      <div class="message-thread__message ${sender}">
        <div class="date pull-right"><@fmt.date date=message.createdDate /></div>
        <div class="message-thread__message__author">${message.sender.fullName}</div>
        <div class="message-thread__message__content">
          <#noescape>${message.formattedMessage}</#noescape>
        </div>
      </div>
    </#list>
  </div>
</#escape>