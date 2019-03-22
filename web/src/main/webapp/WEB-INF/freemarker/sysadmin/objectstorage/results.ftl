<#include "form.ftl" />
<#escape x as x?html>
  <table class="table table-striped">
    <thead>
      <tr>
        <th>ID</th>
        <th>Filename</th>
        <th>Date uploaded</th>
        <th>SHA hash</th>
        <th>Uploader</th>
      </tr>
    </thead>
    <tbody>
      <#list attachments as attachment>
        <tr>
          <td>${attachment.id}</td>
          <td>
            <#if (attachment.asByteSource.encrypted)!false>
              <span class="tabula-tooltip" data-title="Encrypted">
                <i class="fal fa-lock-alt"></i>
              </span>
            </#if>
            <a href="${url('/sysadmin/objectstorage/${attachment.id}')}">${attachment.name}</a>
            <#if attachment.temporary><span class="label label-info">Temporary</span></#if>
          </td>
          <td><@fmt.date date=attachment.dateUploaded /></td>
          <td><code>${attachment.hash}</code></td>
          <td>
            <#if attachment.uploadedBy??>
              <@userlookup id=attachment.uploadedBy>
                <#if returned_user.foundUser>
                  ${returned_user.fullName}
                <#else>
                  ${attachment.uploadedBy}
                </#if>
              </@userlookup>
            </#if>
          </td>
        </tr>
      </#list>
    </tbody>
  </table>
</#escape>