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
      <header>
        <#if department.formattedMitCircsGuidance?has_content>
          <div class="mitcircs-form__guidance mitcircs-form__guidance--departmental">
            <h2>${department.name} guidance</h2>

            <#noescape>${department.formattedMitCircsGuidance!''}</#noescape>
          </div>
        </#if>

        <div class="mitcircs-form__guidance">
          <h2>General guidance</h2>

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
        </div>
      </header>
    </div>

    <article class="col-md-8 col-md-pull-4 mitcircs-form__fields">
      <@f.form id="mitigatingCircumstancesForm" method="POST" modelAttribute="command" class="dirty-check double-submit-protection" enctype="multipart/form-data">
        <#if errors?? && errors.hasErrors()>
          <div class="alert alert-danger">
            <#if errors.hasGlobalErrors()>
              <#list errors.globalErrors as e>
                <div><@spring.message message=e /></div>
              </#list>
            <#else>
              <#-- there were errors but they're all field errors. -->
              <div>There was a problem with your submission that needs fixing, please see the errors below.</div>
            </#if>
          </div>
        </#if>

        <#include "_fields.ftl" />

        <div class="fix-footer">
          <p>
            <button type="submit" class="btn btn-primary" name="approve" value="false">Save draft</button>
            <#if command.self>
              <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#approve-modal">Submit</button>
            </#if>

            <#if submission??>
              <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.viewSubmission submission />">Cancel</a>
            <#else>
              <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.studenthome student />">Cancel</a>
            </#if>
          </p>
        </div>

        <#if command.self>
          <div id="approve-modal" class="modal fade">
            <@modal.wrapper>
              <@modal.header>
                <h6 class="modal-title">Submit mitigating circumstances</h6>
              </@modal.header>
              <@modal.body>
                <p>I confirm that the information I have given is true and that I have read and understood the University Guidance on mitigating circumstances.</p>

                <p>The University reserves the right to check the legitimacy of any evidence provided. If any submission is found to be fabricated or altered then you may be investigated under Regulation 23, Student Disciplinary Offences.</p>
              </@modal.body>
              <@modal.footer>
                <button type="submit"  class="btn btn-primary" name="approve" value="true">Confirm</button>
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
              </@modal.footer>
            </@modal.wrapper>
          </div>
        </#if>
      </@f.form>
    </article>
  </section>
</#escape>