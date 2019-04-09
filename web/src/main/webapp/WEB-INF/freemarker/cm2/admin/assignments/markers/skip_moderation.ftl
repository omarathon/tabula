<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
  <#if nextStagesDescription??>
    <@cm2.assignmentHeader "Skip moderation and send to ${nextStagesDescription?lower_case}" assignment "for" />
  </#if>

  <@f.form method="post" action="${formAction}" modelAttribute="command" cssClass="double-submit-protection">
    <@form.errors path="" />
    <input type="hidden" name="confirmScreen" value="true" />

    <#include "_marking_complete_lists.ftl" />

    <p>
      Feedback for <strong><@fmt.p (command.feedbackForRelease?size) "student" /></strong> will be listed as completed.
      Note that you will not be able to moderate feedback associated with <@fmt.p (command.feedbackForRelease?size) "this student" "these students" "1" "0" false /> after this point.
      If you wish to moderate feedback for <@fmt.p (command.feedbackForRelease?size) "this student" "these students" "1" "0" false /> then click cancel to return to the feedback list.
    </p>

    <@bs3form.form_group>
      <@bs3form.checkbox path="confirm">
        <@f.checkbox path="confirm" /> I confirm that moderation is not required for <@fmt.p markerFeedback?size "this student" "these students" "1" "0" false />
      </@bs3form.checkbox>
    </@bs3form.form_group>

    <div class="buttons submit-buttons">
      <input class="btn btn-primary" type="submit" value="Confirm">
      <a class="btn btn-default" href="<@routes.cm2.listmarkersubmissions assignment marker />">Cancel</a>
    </div>
  </@f.form>
</#escape>