<#escape x as x?html>
  <h1>Declare mitigating circumstances</h1>

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

        <#-- TODO: Departmental-level guidance -->
      </header>
    </div>

    <article class="col-md-8 col-md-pull-4 mitcircs-form__fields">
      <@f.form id="mitigatingCircumstancesForm" method="POST" modelAttribute="command" class="dirty-check double-submit-protection" enctype="multipart/form-data">
        <#include "_fields.ftl" />

        <div class="fix-footer">
          <#assign submitLabel><#if submission??>Update<#else>Submit</#if></#assign>
          <input type="submit" class="btn btn-primary" value="${submitLabel}">
          <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.studenthome command.student />">Cancel</a>
        </div>
      </@f.form>
    </article>
  </section>
</#escape>