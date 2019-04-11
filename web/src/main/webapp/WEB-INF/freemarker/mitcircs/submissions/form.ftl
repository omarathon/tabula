<#escape x as x?html>
  <h1>Declare mitigating circumstances</h1>

  <section class="row mitcircs-form fix-area">
    <div class="col-md-4 col-md-push-8">
      <header class="mitcircs-form__guidance">
        <h2>Guidance</h2>

        <p>Mitigating Circumstances processes are there to support students who have experienced sudden, unforeseen and serious
          accidents, physical or mental health difficulties, or a detrimental change in personal circumstances, that has affected
          a point of summative assessment (essay/examination), and that can be supported with evidence from an external source (e.g.
          GP, hospital, counsellor, death certificate).</p>

        <p>Before putting in a claim for mitigation, please read the
          <a target="_blank" href="https://warwick.ac.uk/services/aro/dar/quality/categories/examinations/policies/u_mitigatingcircumstances/">
            university policy for mitigating circumstances
          </a>.</p>

        <#-- TODO: Departmental-level guidance -->
      </header>
    </div>

    <article class="col-md-8 col-md-pull-4 mitcircs-form__fields">
      <@f.form method="POST" modelAttribute="command" class="dirty-check double-submit-protection" enctype="multipart/form-data">
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