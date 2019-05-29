<#import "mitcirc_form_macros.ftl" as mitcirc />
<#escape x as x?html>
  <h1>Record outcomes for MIT-${submission.key}</h1>

  <section class="mitcircs-form ">

    <article class="mitcircs-form__fields">
      <@f.form id="recordOutcomesForm" method="POST" modelAttribute="command" class="dirty-check double-submit-protection">

        <@mitcirc.question_section
          question = "Mitigation grade"
          hint = "This grading will be shared with exam boards."
        >
          <@mitcirc.radios outcomeGrading "outcomeGrading" />
          <@bs3form.errors path="outcomeGrading" />
        </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Outline why this mitigating circumstances submission has been awarded a particular grade "
          hint = "Robust minutes are required for any consequent appeals or OIA investigations."
        >
          <@bs3form.form_group "outcomeReasons">
            <@f.textarea path="outcomeReasons" cssClass="form-control" rows="5" />
            <@bs3form.errors path="outcomeReasons" />
          </@bs3form.form_group>
        </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Recommendations to the board of examiners"
          hint = "Take into account and reflect relevant factors such as the student’s mode of study, or mode of assessment. The decision will only be based on the evidence that has been presented in the original submission"
        >
          <@mitcirc.checkboxesWithOther boardRecommendations "boardRecommendations" "boardRecommendationOther" />
        </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Additional comments for the board of examiners"
          hint = "Please include any additonal information about the panel's recommendations. Do not include any specifics about the mitigation and only refer to the student by their University ID"
        >
          <@bs3form.form_group "boardRecommendationComments">
            <@f.textarea path="boardRecommendationComments" cssClass="form-control" rows="5" />
            <@bs3form.errors path="boardRecommendationComments" />
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