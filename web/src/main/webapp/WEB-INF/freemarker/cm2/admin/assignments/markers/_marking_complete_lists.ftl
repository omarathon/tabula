<#escape x as x?html>
<@spring.bind path="markerFeedback">
  <@bs3form.errors path="markerFeedback" />
  <#assign markerFeedback=status.actualValue />
  <#assign noContent = command.noContent />
  <#assign noMarks = command.noMarks />
  <#assign noFeedback = command.noFeedback />
  <#assign releasedFeedback = command.releasedFeedback />
  <#assign notReadyToMark = command.notReadyToMark />

  <#if releasedFeedback?has_content>
    <div class="alert alert-info">
      <#assign releasedFeedbackIds>
        <ul><#list releasedFeedback as markerFeedback>
        <li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul></#assign>
      <a class="use-popover"
         data-html="true"
         data-original-title="<span class='text-info'><strong>Already released</strong></span>"
         data-content="${releasedFeedbackIds}">
        <@fmt.p (releasedFeedback?size ) "submission" />
      </a>
      <#if releasedFeedback?size == 1>
        has already been marked as completed. This will be ignored.
      <#else>
        have already been marked as completed. These will be ignored.
      </#if>
    </div>
  </#if>

  <#if notReadyToMark?has_content>
    <div class="alert alert-info">
      <#assign notReadyToMarkIds>
        <ul><#list notReadyToMark as markerFeedback>
        <li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul></#assign>
      <a class="use-popover"
         data-html="true"
         data-original-title="<span class='text-info'><strong>Already released</strong></span>"
         data-content="${notReadyToMarkIds}">
        <@fmt.p (notReadyToMark?size ) "submission" />
      </a>
      <#if notReadyToMark?size == 1>
        is not ready for you to mark. This will be ignored.
      <#else>
        are not ready for you to mark. These will be ignored.
      </#if>
    </div>
  </#if>

  <#if (assignment.collectMarks && noMarks?size > 0)>
    <#assign count><#if (noMarks?size > 1)>  ${noMarks?size} students do<#else>One student does</#if></#assign>
    <#assign noMarksIds>
      <ul><#list noMarks as markerFeedback>
      <li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul></#assign>
    <div class="alert alert-info">
      ${count} not have a mark. You will not be able to add a mark for these students later.
      <a class="use-popover" id="popover-marks" data-html="true"
         data-original-title="<span class='text-info'><strong>No marks</strong></span>"
         data-content="${noMarksIds}">
        <i class="fa fa-question-sign"></i>
      </a>
    </div>
  </#if>

  <#if (noFeedback?size > 0) >
    <#assign count><#if (noFeedback?size > 1)>${noFeedback?size} students do<#else>One student does</#if></#assign>
    <#assign noFilesIds>
      <ul><#list noFeedback as markerFeedback>
          <li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul>
    </#assign>
    <div class="alert alert-info">
      ${count} not have any feedback files attached. You will not be able to add feedback comments or files for this student later.
      <a class="use-popover" id="popover-files" data-html="true"
         data-original-title="<span class='text-info'><strong>No feedback files</strong></span>"
         data-content="${noFilesIds}">
        <i class="fa fa-question-sign"></i>
      </a>
    </div>
  </#if>

  <#if (noContent?has_content)>
    <#assign count><#if (noContent?size > 1)>${noContent?size} students have<#else>One student has</#if></#assign>
    <#assign noContentIds>
      <ul><#list noContent as markerFeedback>
          <li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul>
    </#assign>
    <div class="alert alert-info">
      ${count} not been given a mark. These students will not be sent to the ${nextStagesDescription?lower_case}.
      <a class="use-popover" id="popover-files" data-html="true" aria-label="Help"
         data-original-title="<span class='text-info'><strong>Not marked</strong></span>"
         data-content="${noContentIds}">
        <i class="fa fa-question-circle"></i>
      </a>
    </div>
  </#if>

  <#list markerFeedback as mf>
    <input type="hidden" name="markerFeedback" value="${mf.id}" />
  </#list>
</@spring.bind>
</#escape>
