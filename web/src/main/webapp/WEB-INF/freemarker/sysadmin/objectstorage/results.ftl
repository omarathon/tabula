<#include "form.ftl" />
<#escape x as x?html>
  <#macro row result>
    <#if result.attachment??>
      <#local attachment = result.attachment />
      <tr>
        <td>${attachment.id}</td>
        <td>
          <#if (attachment.asByteSource.encrypted)!false>
            <span tabindex="0" role="button" class="tabula-tooltip" data-title="Encrypted">
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
    <#else>
      <tr>
        <td>${result.id}</td>
        <td>
          <a href="${url('/sysadmin/objectstorage/${result.id}')}">${result.id}</a>
          <span class="label label-danger">De-referenced</span>
        </td>
        <td>&nbsp;</td>
        <td>&nbsp;</td>
        <td>&nbsp;</td>
      </tr>
    </#if>
  </#macro>

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
      <#list results as result>
        <@row result />
      </#list>
    </tbody>
  </table>
</#escape>