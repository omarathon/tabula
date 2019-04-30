<#macro enumListWithOther enumValues otherValue>
  <#list enumValues as value>${value.description}<#if value.entryName == "Other"> (${otherValue})</#if><#if value_has_next>, </#if></#list>
</#macro>

<#macro detail label>
  <div class="row form-horizontal">
    <div class="col-sm-3 control-label">
      ${label}
    </div>
    <div class="col-sm-9">
      <div class="form-control-static">
        <#nested>
      </div>
    </div>
  </div>
</#macro>

<#macro section label>
  <div class="mitcircs-details__section row form-horizontal">
    <div class="control-label">${label}</div>
    <div class="content form-control-static">
      <#nested>
    </div>
  </div>
</#macro>

<#macro asyncSection label url>
  <div class="mitcircs-details__section async row form-vertical" data-href="${url}">
    <div class="control-label">${label}</div>
    <div class="content form-control-static">
      <i class="fas fa-spinner fa-pulse"></i> Loading&hellip;
    </div>
  </div>
</#macro>

<#escape x as x?html>
  <h1>MIT-${submission.key}</h1>
  <section class="mitcircs-details">
    <div class="row">
      <div class="col-sm-6 col-md-7">
        <@detail "State">${submission.state.toString}</@detail>
        <@detail "Issue type"><@enumListWithOther submission.issueTypes submission.issueTypeDetails!"" /></@detail>
        <@detail "Start date"><@fmt.date date=submission.startDate includeTime=false /></@detail>
        <@detail "End date">
          <#if submission.endDate??><@fmt.date date=submission.endDate includeTime=false /><#else><span class="very-subtle">Issue ongoing</span></#if>
        </@detail>
        <#if submission.relatedSubmission??>
          <@detail "Related submission">
            <a href="<@routes.mitcircs.viewsubmission submission.relatedSubmission />">
              MIT-${submission.relatedSubmission.key}
              <@enumListWithOther submission.relatedSubmission.issueTypes submission.relatedSubmission.issueTypeDetails!"" />
            </a>
          </@detail>
        </#if>
        <#if submission.contacted>
          <@detail "Discussed submission with">
            <@enumListWithOther submission.contacts submission.contactOther!"" />
          </@detail>
        <#else>
          <@detail "Reason for not disussing submission">
            ${submission.noContactReason}
          </@detail>
        </#if>
      </div>
      <div class="col-sm-6 col-md-4">
        <div class="row form-horizontal">
          <div class="col-sm-4 control-label">Actions</div>
          <div class="col-sm-8">
            <#if submission.editable>
              <p><a href="<@routes.mitcircs.editsubmission submission />" class="btn btn-default btn-block">Edit submission</a></p>
            </#if>
            <#if submission.evidencePending>
              <p><a href="<@routes.mitcircs.pendingevidence submission />" class="btn btn-default btn-block">Upload pending evidence</a></p>
            </#if>
          </div>
        </div>
      </div>
    </div>
    <@section "Details">
      <#noescape>${submission.formattedReason}</#noescape>
    </@section>
    <@section "Affected assessments">
      <#if submission.affectedAssessments?has_content>
        <table class="table table-default">
          <thead>
          <tr>
            <th class="col-sm-2">Type</th>
            <th class="col-sm-3">Module</th>
            <th class="col-sm-5">Name</th>
            <th class="col-sm-2">Deadline / exam date</th>
          </tr>
          </thead>
          <tbody>
          <#list submission.affectedAssessments as assessment>
            <tr>
              <td><#if assessment.assessmentType.code == "A">Assignment<#else>Exam</#if></td>
              <td>
                <span class="mod-code">
                  ${assessment.module.code?upper_case}</span> <span class="mod-name">${assessment.module.name} (${assessment.academicYear.toString})
                </span>
              </td>
              <td>${assessment.name}</td>
              <td><#if assessment.deadline??><@fmt.date date=assessment.deadline includeTime=false /><#else><span class="very-subtle">Unknown</span></#if></td>
            </tr>
          </#list>
          </tbody>
        </table>
      <#else>
        This issue doesn't affect any assessments
      </#if>
    </@section>
    <#if submission.attachments?has_content>
      <@section "Evidence">
        <ul class="unstyled">
          <#list submission.attachments as attachment>
            <#assign url></#assign>
            <li id="attachment-${attachment.id}" class="attachment">
              <i class="fa fa-file-o"></i>
              <a target="_blank" href="<@routes.mitcircs.renderAttachment submission attachment />"><#compress> ${attachment.name} </#compress></a>&nbsp;
            </li>
          </#list>
        </ul>
      </@section>
    </#if>
    <#if submission.evidencePending>
      <@section "Pending evidence">
        <p>Due date: <@fmt.date date=submission.pendingEvidenceDue includeTime = false /></p>
        <#noescape>${submission.formattedPendingEvidence}</#noescape>
      </@section>
    </#if>
    <#assign messageUrl><@routes.mitcircs.messages submission /></#assign>
    <@asyncSection "Messages" messageUrl />
  </section>
</#escape>