<#macro markerFeedbackSummary stage markerFeedback>
  <div class="well">
    <div class="feedback-summary-heading">
      <h3>${stage.description}'s feedback</h3>
      <h5>
        ${markerFeedback.marker.fullName}
        <#if markerFeedback.updatedOn??>
          <small>- <@fmt.date markerFeedback.updatedOn /></small>
        </#if>
      </h5>
      <div class="clearfix"></div>
    </div>

    <div class="${stage.name} feedback-summary">
      <#if markerFeedback.moderationSkipped>
        Not moderated
      <#elseif markerFeedback.hasContent && !markerFeedback.hasBeenModified>
        Approved by the moderator
      <#else>
        <div class="feedback-details">
          <#if markerFeedback.mark?has_content || markerFeedback.grade?has_content>
            <div class="mark-grade">
              <div>
                <div class="mg-label">
                  Mark:
                </div>
                <div>
                  <span class="mark">${markerFeedback.mark!""}</span>
                  <span>%</span>
                </div>
                <div class="mg-label">
                  Grade:
                </div>
                <div class="grade">${markerFeedback.grade!""}</div>
              </div>
            </div>
          <#else>
            <h5>No mark or grade added.</h5>
          </#if>

          <#local hasFeedback = false>
          <#list assignment.feedbackFields as field>
            <#list markerFeedback.customFormValues as fv>
              <#if fv.name == field.name>
                <#local formValue = fv>
                <#break>
              </#if>
            </#list>

            <#if formValue?? && formValue.value?has_content>
              <#local hasFeedback = true>
              <div class="feedback-comments">
                <h5>${field.label!field.name}</h5>
                <p>${formValue.valueFormattedHtml!""}</p>
              </div>
            </#if>
          </#list>

          <#if !hasFeedback>
            <h5>No feedback comments added.</h5>
          </#if>
        </div>

        <#if markerFeedback.attachments?has_content >
          <div class="feedback-attachments attachments">
            <h5>Attachments</h5>
            <div>
              <#assign downloadMFUrl><@routes.cm2.markerFeedbackFilesDownload markerFeedback/></#assign>
              <@fmt.download_attachments markerFeedback.attachments downloadMFUrl?markup_string "for ${stage.description?uncap_first}" "feedback-${markerFeedback.feedback.studentIdentifier}" />
              <#list markerFeedback.attachments as attachment>
                <input value="${attachment.id}" name="${attachment.name}" type="hidden" />
              </#list>
            </div>
          </div>
        </#if>
        <div style="clear: both;"></div>
      </#if>
    </div>
  </div>
</#macro>

<#macro feedbackSummary feedback>
  <div class="well">
    <div class="feedback-summary-heading">
      <h3>Feedback</h3>
      <#if feedback.updatedDate??>
        <h5>
          Added <@fmt.date feedback.updatedDate />
        </h5>
      </#if>
    </div>

    <div class="feedback-details">
        <#if feedback.actualMark?has_content || feedback.actualGrade?has_content>
          <div class="mark-grade">
            <div>
              <div class="mg-label">
                Mark:
              </div>
              <div>
                <span class="mark">${feedback.actualMark!""}</span>
                <span>%</span>
              </div>
              <div class="mg-label">
                Grade:
              </div>
              <div class="grade">${feedback.actualGrade!""}</div>
            </div>
          </div>
        <#else>
          <h5>No mark or grade added.</h5>
        </#if>

        <#local hasFeedback = false>
        <#list assignment.feedbackFields as field>
            <#list feedback.customFormValues as fv>
                <#if fv.name == field.name>
                    <#local formValue = fv>
                    <#break>
                </#if>
            </#list>

            <#if formValue?? && formValue.value?has_content>
                <#local hasFeedback = true>
              <div class="feedback-comments">
                <h5>${field.label!field.name}</h5>
                <p>${formValue.valueFormattedHtml!""}</p>
              </div>
            </#if>
        </#list>

        <#if !hasFeedback>
          <h5>No feedback comments added.</h5>
        </#if>
    </div>

      <#if feedback.attachments?has_content >
        <div class="feedback-attachments attachments">
          <h5>Attachments</h5>
          <div>
              <#assign downloadMFUrl><@routes.cm2.feedbackFilesDownload feedback/></#assign>
              <@fmt.download_attachments feedback.attachments downloadMFUrl?markup_string  "for ${feedback.studentIdentifier}" "feedback-${feedback.studentIdentifier}" />
              <#list feedback.attachments as attachment>
                <input value="${attachment.id}" name="${attachment.name}" type="hidden" />
              </#list>
          </div>
        </div>
      </#if>
    <div style="clear: both;"></div>
    </div>
</#macro>
