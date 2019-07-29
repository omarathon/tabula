<#import "mitcirc_form_macros.ftl" as mitcirc />
<#import "*/mitcircs_components.ftlh" as components />

<h1>Sensitive information for MIT-${submission.key}</h1>

<div class="col-md-4 col-md-push-8">
  <section class="mitcircs-details">
      <@components.studentSummary submission true />
      <@components.submissionSummary submission true />
  </section>
</div>
<div class="col-md-8 col-md-pull-4">
  <@f.form id="sensitiveEvidenceForm" method="POST" modelAttribute="command" class="dirty-check double-submit-protection" enctype="multipart/form-data">

    <@mitcirc.question_section
      question = "Provide additional information where the student does not want to submit highly sensitive information."
      hint="Confirm that you have seen supporting evidence relevant to this mitigating circumstances submission that was too sensitive to be submitted by the student."
      cssClass = "form-horizontal"
      showNumber = false
    >

      <@bs3form.form_group "sensitiveEvidenceComments">
        <@bs3form.label path="sensitiveEvidenceComments" cssClass="col-xs-4 col-sm-2">Comments</@bs3form.label>
        <div class="col-xs-8 col-sm-10">
          <@f.textarea path="sensitiveEvidenceComments" cssClass="form-control" rows="5" />
          <@bs3form.errors path="sensitiveEvidenceComments" />
        </div>
      </@bs3form.form_group>

      <@bs3form.form_group "sensitiveEvidenceSeenOn">
        <@bs3form.label path="sensitiveEvidenceSeenOn" cssClass="col-xs-4 col-sm-2">Seen on</@bs3form.label>

        <div class="col-xs-8 col-sm-4">
          <@spring.bind path="sensitiveEvidenceSeenOn">
            <div class="input-group">
              <@f.input path="sensitiveEvidenceSeenOn" autocomplete="off" cssClass="form-control date-picker" />
              <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
            </div>
          </@spring.bind>

          <@bs3form.errors path="sensitiveEvidenceSeenOn" />
        </div>
      </@bs3form.form_group>

    </@mitcirc.question_section>

    <div class="fix-footer">
      <button type="submit" class="btn btn-primary">Confirm</button>
      <a class="btn btn-default dirty-check-ignore" href="<#if fromPanel && submission.panel??><@routes.mitcircs.reviewSubmissionPanel submission /><#else><@routes.mitcircs.reviewSubmission submission /></#if>">Cancel</a>
    </div>

  </@f.form>
</div>