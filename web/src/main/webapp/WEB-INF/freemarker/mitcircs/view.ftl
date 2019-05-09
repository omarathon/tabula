<#import "*/mitcircs_components.ftl" as components />

<#escape x as x?html>
  <h1>MIT-${submission.key}</h1>
  <section class="mitcircs-details">
    <div class="row">
      <div class="col-sm-6 col-md-7">
        <@components.detail "State">${submission.state.description}</@components.detail>
        <@components.detail "Issue type"><@components.enumListWithOther submission.issueTypes submission.issueTypeDetails!"" /></@components.detail>
        <@components.detail "Start date"><@fmt.date date=submission.startDate includeTime=false /></@components.detail>
        <@components.detail "End date">
          <#if submission.endDate??><@fmt.date date=submission.endDate includeTime=false /><#else><span class="very-subtle">Issue ongoing</span></#if>
        </@components.detail>
        <#if submission.relatedSubmission??>
          <@components.detail "Related submission">
            <a href="<@routes.mitcircs.viewSubmission submission.relatedSubmission />">
              MIT-${submission.relatedSubmission.key}
              <@components.enumListWithOther submission.relatedSubmission.issueTypes submission.relatedSubmission.issueTypeDetails!"" />
            </a>
          </@components.detail>
        </#if>
        <#if submission.contacted>
          <@components.detail "Discussed submission with">
            <@components.enumListWithOther submission.contacts submission.contactOther!"" />
          </@components.detail>
        <#else>
          <@components.detail "Reason for not discussing submission">
            ${submission.noContactReason}
          </@components.detail>
        </#if>
      </div>
      <div class="col-sm-6 col-md-4">
        <div class="row form-horizontal">
          <div class="col-sm-4 control-label">Actions</div>
          <div class="col-sm-8">
            <#if submission.isEditable(user.apparentUser)>
              <p><a href="<@routes.mitcircs.editSubmission submission />" class="btn btn-default btn-block">Edit submission</a></p>
            </#if>
            <#if isSelf && submission.evidencePending>
              <p><a href="<@routes.mitcircs.pendingEvidence submission />" class="btn btn-default btn-block">Upload pending evidence</a></p>
            </#if>
          </div>
        </div>
      </div>
    </div>
    <@components.section "Details">
      <#noescape>${submission.formattedReason}</#noescape>
    </@components.section>
    <@components.section "Affected assessments">
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
    </@components.section>
    <#if submission.attachments?has_content>
      <@components.section "Evidence">
        <ul class="unstyled">
          <#list submission.attachments as attachment>
            <#assign url></#assign>
            <li id="attachment-${attachment.id}" class="attachment">
              <i class="fa fa-file-o"></i>
              <a target="_blank" href="<@routes.mitcircs.renderAttachment submission attachment />"><#compress> ${attachment.name} </#compress></a>&nbsp;
            </li>
          </#list>
        </ul>
      </@components.section>
    </#if>
    <#if submission.evidencePending>
      <@components.section "Pending evidence">
        <p>Due date: <@fmt.date date=submission.pendingEvidenceDue includeTime = false /></p>
        <#noescape>${submission.formattedPendingEvidence}</#noescape>
      </@components.section>
    </#if>
    <#if submission.sensitiveEvidenceComments?has_content>
      <@components.section "Sensitive evidence">
        <p>Seen by: ${submission.sensitiveEvidenceSeenBy.fullName} on <@fmt.date date=submission.pendingEvidenceDue includeTime = false /></p>
        <#noescape>${submission.formattedSensitiveEvidenceComments}</#noescape>
      </@components.section>
    </#if>
    <#assign messageUrl><@routes.mitcircs.messages submission /></#assign>
    <@components.asyncSection "messages" "Messages" messageUrl />
  </section>
</#escape>