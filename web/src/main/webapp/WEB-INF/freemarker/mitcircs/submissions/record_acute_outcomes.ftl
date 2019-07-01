<#import "mitcirc_form_macros.ftl" as mitcirc />
<#import "*/mitcircs_components.ftl" as components />

<#escape x as x?html>
  <h1>Record acute outcomes for MIT-${submission.key}</h1>

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
          cssClass = "mitcircs-form__fields__section__optional-question"
        >
          <#list command.affectedAssessments as assessment>
            <@spring.nestedPath path="affectedAssessments[${assessment_index}]">
              <@f.hidden path="name" />
              <@f.hidden path="module" />
              <@f.hidden path="moduleCode" />
              <@f.hidden path="sequence" />
              <@f.hidden path="academicYear" />
              <@f.hidden path="assessmentType" />
              <@f.hidden path="deadline" />
              <#if assessment.assessmentType.code == "A">
                <div class="checkbox">
                  <label>
                    <@f.checkbox path="acuteOutcomeApplies" value="true" />
                    <@components.assessmentModule assessment=assessment formatted=false /> &mdash; ${assessment.name}
                  </label>
                </div>
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
</#escape>