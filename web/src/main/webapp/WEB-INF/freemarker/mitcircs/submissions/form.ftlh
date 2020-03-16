<#import "*/modal_macros.ftlh" as modal />
<#import "*/mitcircs_components.ftlh" as components />
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
            <#noautoesc>${department.formattedMitCircsGuidance!''}</#noautoesc>
          </div>
        </#if>

      <div class="mitcircs-form__guidance">
        <h2>General guidance</h2>

        <p><i class="fas fa-play-circle">&nbsp;</i><a href="#" data-toggle="modal" data-target="#guidance-video-modal" id="mitigatingCircumstancesVideo">How to declare mitigating circumstances</a></p>

        <@modal.modal id="guidance-video-modal">
          <@modal.wrapper>
            <@modal.header></@modal.header>
            <@modal.body>
              <video poster="https://warwick.ac.uk/services/its/servicessupport/web/tabula/manual/cm2/mit-circs/declare/mc-students.jpg" controls preload="metadata" width="1920" height="1080">
                <source src="https://warwick.ac.uk/services/its/servicessupport/web/tabula/manual/cm2/mit-circs/declare/mc-students.mp4" type="video/mp4" width="1920" height="1080">
                <source src="https://warwick.ac.uk/services/its/servicessupport/web/tabula/manual/cm2/mit-circs/declare/mc-students.webm" type="video/webm" width="1920" height="1080">
              </video>
            </@modal.body>
          </@modal.wrapper>
        </@modal.modal>

        <p>
          Mitigating circumstances processes support students who have experienced sudden, unforeseen and serious issues such as an accident,
          physical or mental health difficulties, or a detrimental change in personal circumstances. Sometimes issues like these can affect
          your coursework or exams, and if so, you should tell us about them. You are normally expected to provide evidence from an external
          source (eg your GP, hospital, counsellor) to support your submission.
        </p>

        <#if features.mitcircsCovid19>
          <p>
            We are currently dealing with coronavirus related mitigation in this system.  For coronavirus mitigation the need for external evidence
            is being relaxed, so you should tell us about your claim even if you cannot currently provide evidence about your issues.
          </p>

          <p>
            Before putting in a claim for mitigation for coronavirus related issues, please read the
            <a href="https://warwick.ac.uk/insite/coronavirus/" target="_blank">current university FAQs on coronavirus</a>.
          </p>

          <p>
            <a href="https://www.gov.uk/government/publications/covid-19-stay-at-home-guidance/stay-at-home-guidance-for-people-with-confirmed-or-possible-coronavirus-covid-19-infection" target="_blank">Current UK government advice on coronavirus</a>
          </p>

          <p>
            <a href="https://111.nhs.uk/service/COVID-19/" target="_blank">Current NHS111 advice on coronavirus</a>
          </p>
        <#else>
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
            deciding final year degree classifications in borderline cases, but it does not automatically result in a higher degree classification.
          </p>
        </#if>
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
            <ul>
              <#list errors.fieldErrors as e>
                <li><@spring.message message=e /></li>
              </#list>
            </ul>
          </#if>
        </div>
      </#if>

      <#include "_fields.ftlh" />

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
        <@modal.modal id="approve-modal">
          <@modal.wrapper>
            <@modal.header>
              <h6 class="modal-title">Submit mitigating circumstances</h6>
            </@modal.header>
            <@modal.body>
                <p>
                  I confirm that the information I have given is true and that I have read and understood:
                  <ul>
                    <li>the University guidance on mitigating circumstances</li>
                    <li>the <a data-toggle="collapse" href="#privacyStatement" role="button" aria-expanded="false" aria-controls="privacyStatement">privacy statement</a> in particular how the information I provide will be used</li>
                  </ul>
                </p>
                <p>The University reserves the right to check the legitimacy of any evidence provided. If any submission is found to be fabricated or altered then you may be investigated under Regulation 23, Student Disciplinary Offences.</p>
              <div class="well collapse" id="privacyStatement">
                <h6>Privacy statement</h6>
                <@components.privacyStatement />
              </div>
            </@modal.body>
            <@modal.footer>
              <button type="submit"  class="btn btn-primary" name="approve" value="true">Confirm</button>
              <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
            </@modal.footer>
          </@modal.wrapper>
        </@modal.modal>
      </#if>
    </@f.form>
  </article>
</section>
