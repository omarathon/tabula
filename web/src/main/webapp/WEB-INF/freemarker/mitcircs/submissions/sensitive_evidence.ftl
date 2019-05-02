<#import "mitcirc_form_macros.ftl" as mitcirc />

<#escape x as x?html>
  <h1>Sensitive information for MIT-${submission.key}</h1>
  <p>Provide additional information where the student does not want to disclose highly sensitive information.</p>

  <@f.form id="sensitiveEvidenceForm" method="POST" modelAttribute="command" class="dirty-check double-submit-protection" enctype="multipart/form-data">
    <@mitcirc.question_section
      question = "Comments"
      hint="Confirm that you have seen supporting evidence relivent to this mitigating circumstances submission that was too sensitive to be submitted by the student."
    >
      <@bs3form.form_group "sensitiveEvidenceComments">
        <@f.textarea path="sensitiveEvidenceComments" cssClass="form-control" rows="5" />
        <@bs3form.errors path="sensitiveEvidenceComments" />
      </@bs3form.form_group>
    </@mitcirc.question_section>

    <div class="fix-footer">
      <button type="submit" class="btn btn-primary">Confirm</button>
      <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.reviewSubmission submission />">Cancel</a>
    </div>

  </@f.form>
</#escape>