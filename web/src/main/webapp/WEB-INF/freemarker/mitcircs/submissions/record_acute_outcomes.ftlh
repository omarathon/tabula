<#import "mitcirc_form_macros.ftl" as mitcirc />
<#import "*/mitcircs_components.ftlh" as components />

<h1>Record acute outcomes for MIT-${submission.key}</h1>

<div class="col-md-4 col-md-push-8">
  <section class="mitcircs-details">
      <@components.studentSummary submission true />
      <@components.submissionSummary submission true />
  </section>
</div>
<div class="col-md-8 col-md-pull-4">
  <section class="mitcircs-form ">

    <article class="mitcircs-form__fields">
      <@f.form id="recordAcuteOutcomesForm" method="POST" modelAttribute="command" class="mitcircs-outcomes-form dirty-check double-submit-protection">

        <@mitcirc.question_section
          question = "Mitigation grade"
          hint = "This grading will be shared with exam boards."
          >
          <@mitcirc.radios outcomeGrading "outcomeGrading" />
          <div class="mitcircs-form__fields__section__nested-checkboxes collapse" data-target=":input[name=outcomeGrading]" data-target-value="Rejected">
            <@mitcirc.checkboxesWithOther rejectionReasons "rejectionReasons" "rejectionReasonsOther"/>
          </div>
        </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Outline why this mitigating circumstances submission has been awarded a particular grade "
          hint = "Robust notes are required for any consequent appeals or OIA investigations."
        >
          <@bs3form.form_group "outcomeReasons">
            <@f.textarea path="outcomeReasons" cssClass="form-control" rows="5" />
            <@bs3form.errors path="outcomeReasons" />
          </@bs3form.form_group>
        </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Outcome of mitigation"
          hint = "Take into account and reflect relevant factors such as the student’s mode of study, or mode of assessment. The decision will only be based on the evidence that has been presented in the original submission"
          cssClass = "mitcircs-form__fields__section__optional-question"
        >
          <@mitcirc.radios acuteOutcomes "acuteOutcome" />
        </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Affected assessments"
          hint = "Please select all of the assessments that this mitigation should apply to."
          cssClass = "mitcircs-form__fields__section__optional-question mitcircs-form__fields__section__affected-assessments"
        >
          <#assign hasAssignments = false />
          <#list command.affectedAssessments as assessment>
            <@spring.nestedPath path="affectedAssessments[${assessment_index}]">
              <#if assessment.assessmentType.code != "E">
                <#if !hasAssignments>
                  <div class="checkbox row grant-extensions collapse">
                    <div class="col-md-7"><strong>Assessment</strong></div>
                    <div class="col-md-2"><strong>Deadline</strong></div>
                    <div class="col-md-3"><strong>Extension deadline</strong></div>
                  </div>
                  <#assign hasAssignments = true />
                </#if>
                <div class="checkbox row">
                  <@f.hidden path="name" />
                  <@f.hidden path="module" />
                  <@f.hidden path="moduleCode" />
                  <@f.hidden path="sequence" />
                  <@f.hidden path="academicYear" />
                  <@f.hidden path="assessmentType" />
                  <@f.hidden path="deadline" />
                  <label class="col-md-7">
                    <@f.checkbox path="acuteOutcomeApplies" value="true" />
                    <@components.assessmentModule assessment=assessment formatted=false /> &mdash; ${assessment.name}
                  </label>
                  <div class="col-md-2 grant-extensions collapse">
                    <#if assessment.deadline??><@fmt.date date=assessment.deadline shortMonth=true includeTime=false /><#else><span class="very-subtle">Unknown</span></#if>
                  </div>
                  <div class="col-md-3 grant-extensions collapse">
                    <div class="input-group extensionDeadlineContainer">
                      <@f.input path="extensionDeadline" autocomplete="off" cssClass="form-control other-input date-time-picker" />
                      <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
                    </div>
                    <@bs3form.errors path="extensionDeadline" />
                  </div>
                </div>
              </#if>
            </@spring.nestedPath>
          </#list>
          <#-- Hide the exam components at the end so they don't mess with the checkbox styles -->
          <#list command.affectedAssessments as assessment>
            <@spring.nestedPath path="affectedAssessments[${assessment_index}]">
              <#if assessment.assessmentType.code != "A">
                <@f.hidden path="name" />
                <@f.hidden path="module" />
                <@f.hidden path="moduleCode" />
                <@f.hidden path="sequence" />
                <@f.hidden path="academicYear" />
                <@f.hidden path="assessmentType" />
                <@f.hidden path="deadline" />
              </#if>
            </@spring.nestedPath>
          </#list>
        </@mitcirc.question_section>

        <div class="fix-footer">
          <#if submission.state.entryName != "Outcomes Recorded">
            <button type="submit" class="btn btn-primary" name="confirm" value="false">Save as draft</button>
          </#if>
          <button type="submit" class="btn btn-primary" name="confirm" value="true">Submit</button>
          <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.reviewSubmission submission />">Cancel</a>
        </div>

      </@f.form>
    </article>
  </section>
</div>