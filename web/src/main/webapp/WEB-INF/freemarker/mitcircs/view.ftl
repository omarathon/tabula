<#import "*/mitcircs_components.ftl" as components />
<#assign canModify = can.do("MitigatingCircumstancesSubmission.Modify", submission) />
<#assign isSelf = submission.student.universityId == user.universityId />

<#escape x as x?html>
  <h1>Mitigating circumstances submission MIT-${submission.key}</h1>
  <section class="mitcircs-details">
    <div class="row">
      <div class="col-sm-6 col-md-7">
        <#-- Don't show students states that they have no business seeing -->
        <@components.detail label="State" condensed=true>
          <#if submission.state.entryName == 'Draft'>
            Draft
            <#if isSelf>
              <a href="<@routes.mitcircs.editSubmission submission />" class="btn btn-primary btn-xs">Edit &amp; submit</a></p>
            </#if>
          <#elseif submission.state.entryName == 'Created On Behalf Of Student'>
            Needs sign-off
            <#if isSelf>
              <a href="<@routes.mitcircs.editSubmission submission />" class="btn btn-primary btn-xs">Edit &amp; submit</a></p>
            </#if>
          <#elseif submission.state.entryName == 'Withdrawn'>
            Withdrawn
          <#elseif submission.state.entryName == 'Outcomes Recorded'>
            Decision made
          <#else>
            Submitted
          </#if>
        </@components.detail>
        <@components.detail label="Issue type" condensed=true><@components.enumListWithOther enumValues=submission.issueTypes otherValue=submission.issueTypeDetails!"" condensed=false /></@components.detail>
        <@components.detail label="Start date" condensed=true><#if submission.startDate??><@fmt.date date=submission.startDate includeTime=false /><#else><span class="very-subtle">TBC</span></#if></@components.detail>
        <@components.detail label="End date" condensed=true>
          <#if submission.endDate??><@fmt.date date=submission.endDate includeTime=false /><#else><span class="very-subtle">Issue ongoing</span></#if>
        </@components.detail>
        <#if submission.relatedSubmission??>
          <@components.detail label="Related submission" condensed=true>
            <a href="<@routes.mitcircs.viewSubmission submission.relatedSubmission />">
              MIT-${submission.relatedSubmission.key}
              <@components.enumListWithOther submission.relatedSubmission.issueTypes submission.relatedSubmission.issueTypeDetails!"" />
            </a>
          </@components.detail>
        </#if>
        <#if submission.contacted??>
          <#if submission.contacted>
            <@components.detail "Discussed submission with">
              <@components.enumListWithOther enumValues=submission.contacts otherValue=submission.contactOther!"" condensed=false />
            </@components.detail>
          <#else>
            <@components.detail "Reason for not discussing submission">
              ${submission.noContactReason}
            </@components.detail>
          </#if>
        <#else>
          <@components.detail "Discussed submission with">
            <span class="very-subtle">TBC</span>
          </@components.detail>
        </#if>
      </div>
      <div class="col-sm-6 col-md-5 col-lg-4">
        <div class="row form-horizontal">
          <div class="col-sm-4 control-label">Actions</div>
          <div class="col-sm-8">
            <p><a href="<@routes.mitcircs.studenthome submission.student />" class="btn btn-default btn-block"><i class="fal fa-long-arrow-left"></i> Return to list of submissions</a></p>

            <#if canModify>
              <#if submission.isEditable(user.apparentUser)>
                <#if isSelf && submission.draft>
                  <p><a href="<@routes.mitcircs.editSubmission submission />" class="btn btn-primary btn-block">Edit &amp; submit submission</a></p>
                <#else>
                  <p><a href="<@routes.mitcircs.editSubmission submission />" class="btn btn-default btn-block">Edit submission</a></p>
                </#if>
              </#if>

              <#if isSelf && submission.canWithdraw && !submission.withdrawn>
                <p><a href="<@routes.mitcircs.withdrawSubmission submission />" class="btn btn-default btn-block">Withdraw submission</a></p>
              <#elseif isSelf && submission.canReopen>
                <p><a href="<@routes.mitcircs.reopenSubmission submission />" class="btn btn-default btn-block">Re-open submission</a></p>
              </#if>

              <#if isSelf && submission.evidencePending>
                <p><a href="<@routes.mitcircs.pendingEvidence submission />" class="btn btn-default btn-block">Upload pending evidence</a></p>
              </#if>

              <#if submission.draft && can.do("MitigatingCircumstancesSubmission.Share", submission)>
                <p><a href="<@routes.mitcircs.shareSubmission submission />" class="btn btn-default btn-block">Share submission</a></p>
              </#if>
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
              <td><@components.assessmentType assessment /></td>
              <td><@components.assessmentModule assessment /></td>
              <td>${assessment.name}</td>
              <td><#if assessment.deadline??><@fmt.date date=assessment.deadline includeTime=false shortMonth=true excludeCurrentYear=true /><#else><span class="very-subtle">Unknown</span></#if></td>
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
        <@components.attachments submission />
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
        <p>Seen by: ${submission.sensitiveEvidenceSeenBy.fullName} on <@fmt.date date=submission.sensitiveEvidenceSeenOn includeTime = false /></p>
        <#noescape>${submission.formattedSensitiveEvidenceComments}</#noescape>
      </@components.section>
    <#elseif submission.hasSensitiveEvidence>
      <@components.section "Sensitive evidence">
        <p>There is sensitive evidence that relates to this submission that needs to be discussed in person.</p>
      </@components.section>
    </#if>

    <#if !submission.draft && !submission.withdrawn>
      <#assign messageUrl><@routes.mitcircs.messages submission /></#assign>
      <@components.asyncSection "messages" "Messages" messageUrl />
    </#if>
  </section>
</#escape>