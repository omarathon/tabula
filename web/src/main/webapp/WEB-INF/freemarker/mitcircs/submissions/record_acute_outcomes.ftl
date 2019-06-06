<#import "mitcirc_form_macros.ftl" as mitcirc />
<#escape x as x?html>
  <h1>Record outcomes for MIT-${submission.key}</h1>

  <section class="mitcircs-form ">

    <article class="mitcircs-form__fields">
      <@f.form id="recordAcuteOutcomesForm" method="POST" modelAttribute="command" class="mitcircs-outcomes-form dirty-check double-submit-protection">

        <@mitcirc.question_section
          question = "Outcome of mitigation"
          hint = "Take into account and reflect relevant factors such as the student’s mode of study, or mode of assessment. The decision will only be based on the evidence that has been presented in the original submission"
        >
              <@mitcirc.radios acuteOutcomes "acuteOutcome" />
              <@bs3form.errors path="acuteOutcome" />
          </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Mitigation grade"
          hint = "This grading will be shared with exam boards."
        >
          <@mitcirc.radios outcomeGrading "outcomeGrading" />
          <div class="mitcircs-outcomes-form__rejection-reasons collapse">
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

        <div class="fix-footer">
          <button type="submit"  class="btn btn-primary">Submit</button>
          <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.reviewSubmission submission />">Cancel</a>
        </div>

      </@f.form>
    </article>
  </section>
</#escape>