<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
  <#if nextStagesDescription??>
    <@cm2.assignmentHeader "Send to ${nextStagesDescription?lower_case}" assignment "for" />
  </#if>

  <@f.form method="post" action="${formAction}" modelAttribute="command" cssClass="double-submit-protection">
    <@form.errors path="" />
    <input type="hidden" name="confirmScreen" value="true" />

    <#include "_marking_complete_lists.ftl" />

    <p>
      Feedback for <strong><@fmt.p (command.feedbackForRelease?size) "student" /></strong> will be listed as completed.
      Note that you will not be able to make any further changes to the marks or feedback associated with these students after this point.
      If there are still changes that have to be made for these students then click cancel to return to the feedback list.
    </p>

    <@bs3form.form_group>
      <@bs3form.checkbox path="confirm">
        <@f.checkbox path="confirm" /> I confirm that I have completed marking for <@fmt.p markerFeedback?size "this student" "these students" "1" "0" false />
      </@bs3form.checkbox>
    </@bs3form.form_group>

    <div class="buttons submit-buttons">
      <input class="btn btn-primary" type="submit" value="Confirm">
      <a class="btn btn-default" href="<@routes.cm2.listmarkersubmissions assignment marker />">Cancel</a>
    </div>
  </@f.form>
</#escape>