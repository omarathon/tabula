<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>
  <h1>Declare mitigating circumstances<#if !command.self> on behalf of ${student.fullName}</#if></h1>

  <#if lastUpdatedByOther?? && lastUpdatedByOther>
    <div class="alert alert-info">
      Information was added to this mitigating circumstances submission by ${submission.lastModifiedBy.fullName} at <@fmt.date date=submission.lastModified capitalise=false at=true />.
      You can review the submission and make any necessary changes. In order for this submission to be considered you must first submit it.
    </div>
  <#elseif command.self && submission?? && submission.draft>
    <div class="alert alert-info">
      This is a draft submission. You can review the submission and make any necessary changes. In order for this submission to be considered you must first submit it.
    </div>
  </#if>

  <section class="row mitcircs-form fix-area">
    <div class="col-md-4 col-md-push-8">
      <header class="mitcircs-form__guidance">
        <h2>Guidance</h2>

        <p>
          Mitigating circumstances processes support students who have experienced sudden, unforeseen and serious issues such as an accident,
          physical or mental health difficulties, or a detrimental change in personal circumstances. Sometimes issues like these can affect
          your coursework or exams, and if so, and if you can provide evidence from an external source (eg your GP, hospital, counsellor,
          death certificate), then you should tell us about them.
        </p>

        <p>
          Before putting in a claim for mitigation, please read the
          <a target="_blank" href="https://warwick.ac.uk/mitigatingcircumstances">university policy for mitigating circumstances</a>.
          Mitigating circumstances do not result in changes to marks; they may result in you being able to take a failed assessment again
          as a first attempt, or to have an extension for a deadline, or to have a late penalty removed. Mitigation may also be considered when
          deciding final year degree classifications in borderlines cases, but it does not automatically result in a higher degree classification.
        </p>

        <#noescape>${department.formattedMitCircsGuidance!''}</#noescape>
      </header>
    </div>

    <article class="col-md-8 col-md-pull-4 mitcircs-form__fields">
      <@f.form id="mitigatingCircumstancesForm" method="POST" modelAttribute="command" class="dirty-check double-submit-protection" enctype="multipart/form-data">
        <#include "_fields.ftl" />

        <div class="fix-footer">
          <button type="submit" class="btn btn-primary" name="approve" value="false">Save draft</button>
          <#if command.self>
            <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#approve-modal">Submit</button>
            <div id="approve-modal" class="modal fade">
              <@modal.wrapper>
                <@modal.header>
                  <h6 class="modal-title">Submit mitigating circumstances</h6>
                </@modal.header>
                <@modal.body>
                  I confirm that the information I have given is true and that I have read and understood the University Guidance on mitigating circumstances.
                </@modal.body>
                <@modal.footer>
                  <button type="submit"  class="btn btn-primary" name="approve" value="true">Confirm</button>
                  <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                </@modal.footer>
              </@modal.wrapper>
            </div>
          </#if>
          <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.viewsubmission submission />">Cancel</a>
        </div>
      </@f.form>
    </article>
  </section>
</#escape>