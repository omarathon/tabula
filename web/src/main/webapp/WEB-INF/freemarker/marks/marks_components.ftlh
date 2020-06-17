<#ftl strip_text=true />

<#-- FIXME why is this necessary? -->
<#if JspTaglibs??>
  <#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
  <#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
</#if>

<#import "*/modal_macros.ftlh" as modal />

<#macro moduleMarkStatusIcon student>
  <#if student.needsWritingToSits>
    <span class="tabula-tooltip" data-title="Waiting to be written to SITS">
      <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
      <span class="sr-only">Waiting to be written to SITS</span>
    </span>
  <#elseif student.outOfSync>
    <span class="tabula-tooltip" data-title="Mark is out of sync with current mark in SITS">
      <i class="fa-fw fad fa-exclamation-circle" aria-hidden="true"></i>
      <span class="sr-only">Mark is out of sync with current mark in SITS</span>
    </span>
  <#elseif student.agreed>
    <span class="tabula-tooltip" data-title="Agreed mark published to student">
      <i class="fa-fw fad fa-check-circle" aria-hidden="true"></i>
      <span class="sr-only">Agreed mark published to student</span>
    </span>
  </#if>
</#macro>

<#macro componentMarkStatusIcon marks>
  <#if marks.needsWritingToSits>
    <span class="tabula-tooltip" data-title="Waiting to be written to SITS">
      <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
      <span class="sr-only">Waiting to be written to SITS</span>
    </span>
  <#elseif marks.outOfSync>
    <span class="tabula-tooltip" data-title="Mark is out of sync with current mark in SITS">
      <i class="fa-fw fad fa-exclamation-circle" aria-hidden="true"></i>
      <span class="sr-only">Mark is out of sync with current mark in SITS</span>
    </span>
  <#elseif marks.agreed>
    <span class="tabula-tooltip" data-title="Agreed mark published to student">
      <i class="fa-fw fad fa-check-circle" aria-hidden="true"></i>
      <span class="sr-only">Agreed mark published to student</span>
    </span>
  </#if>
</#macro>

<#macro moduleMarkHistory student member>
  <#if student.history?has_content>
    <button type="button" class="btn btn-default btn-xs tabula-tooltip" data-title="View history" data-toggle="modal" data-target="#mark-history-modal-${student.sprCode?replace('/', '_')}"><i class="fal fa-history" aria-hidden="true"></i><span class="sr-only">View history</span></button>

    <@modal.modal id="mark-history-modal-${student.sprCode?replace('/', '_')}">
      <@modal.wrapper cssClass="modal-lg">
        <@modal.header>
          <h3 class="modal-title">Mark history for ${member.fullName}, ${student.sprCode}</h3>
        </@modal.header>
        <@modal.body>
          <table class="table table-condensed table-striped">
            <thead>
              <tr>
                <th class="col-sm-3">Date</th>
                <th class="col-sm-3">User</th>
                <th class="col-sm-1">Mark</th>
                <th class="col-sm-1">Grade</th>
                <th class="col-sm-1">Result</th>
                <th class="col-sm-3">Comments</th>
              </tr>
            </thead>
            <tbody>
              <#list student.history as history>
                <tr>
                  <td>
                    <@fmt.date date=history.updatedDate />
                  </td>
                  <td>${history.updatedBy.fullName!(history.updatedBy.userId)}</td>
                  <td>${history.mark!'-'}</td>
                  <td>${history.grade!'-'}</td>
                  <td>${(history.result.description)!'-'}</td>
                  <td>${history.comments!}</td>
                </tr>
              </#list>
            </tbody>
          </table>
        </@modal.body>
        <@modal.footer>
          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </@modal.footer>
      </@modal.wrapper>
    </@modal.modal>
  </#if>
</#macro>

<#macro componentMarkHistory marks ac member>
  <#if marks.history?has_content>
    <button type="button" class="btn btn-default btn-xs tabula-tooltip" data-title="View history" data-toggle="modal" data-target="#mark-history-modal-${marks.universityId}-${ac.sequence}"><i class="fal fa-history" aria-hidden="true"></i><span class="sr-only">View history</span></button>

    <@modal.modal id="mark-history-modal-${marks.universityId}-${ac.sequence}">
      <@modal.wrapper cssClass="modal-lg">
        <@modal.header>
          <h3 class="modal-title">Mark history for ${member.fullName}, ${marks.universityId}, ${ac.name}</h3>
        </@modal.header>
        <@modal.body>
          <table class="table table-condensed table-striped">
            <thead>
              <tr>
                <th class="col-sm-3">Date</th>
                <th class="col-sm-3">User</th>
                <th class="col-sm-1">Mark</th>
                <th class="col-sm-1">Grade</th>
                <th class="col-sm-3">Comments</th>
              </tr>
            </thead>
            <tbody>
              <#list marks.history as history>
                <tr>
                  <td>
                    <@fmt.date date=history.updatedDate />
                  </td>
                  <td>${history.updatedBy.fullName!(history.updatedBy.userId)}</td>
                  <td>${history.mark!'-'}</td>
                  <td>${history.grade!'-'}</td>
                  <td>${history.comments!}</td>
                </tr>
              </#list>
            </tbody>
          </table>
        </@modal.body>
        <@modal.footer>
          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </@modal.footer>
      </@modal.wrapper>
    </@modal.modal>
  </#if>
</#macro>
