<#escape x as x?html>
  <#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
  <#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />
  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

  <h1>Deregistered students</h1>
  <h4><span class="muted">for</span> ${smallGroupSet.name}</h4>

  <p>The following students have been deregistered for groups in ${smallGroupSet.name}:</p>

  <table class="table table-striped table-condensed table-hover table-checkable">
    <thead>
    <tr>
      <th>First name</th>
      <th>Last name</th>
      <th>University ID</th>
      <th>Group</th>
    </tr>
    </thead>
    <tbody><#list removed as studentDetails>
      <#assign student = studentDetails.student />
      <#assign group = studentDetails.group />
      <tr>
        <td>${student.firstName}</td>
        <td>${student.lastName}</td>
        <td>${student.universityId} <@pl.profile_link student.universityId /></td>
        <td>${group.name}</td>
      </tr>
    </#list></tbody>
  </table>

  <p><a href="<#noescape>${returnTo}</#noescape>" class="btn btn-default">Return</a></p>
</#escape>