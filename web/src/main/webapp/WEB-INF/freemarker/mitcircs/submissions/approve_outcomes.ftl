<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />

<#escape x as x?html>
  <#assign formAction><@routes.mitcircs.approveOutcomes submission /></#assign>
  <@f.form id="approveOutcomesForm" action="${formAction}" method="POST" modelAttribute="command" class="dirty-check double-submit-protection">
    <#if submission.state.entryName == "Approved By Chair">
      <@modal.header>
        <h3 class="modal-title">Unapprove outcomes for MIT-${submission.key}</h3>
      </@modal.header>
      <@modal.body>
        Please confirm that the outcomes recorded against this submission need to be reviewed.
        Further changes to the outcomes will then be possible.
        <@f.hidden path="approve" value="false"/>
      </@modal.body>
    <#else>
      <@modal.header>
        <h3 class="modal-title">Approve outcomes for MIT-${submission.key}</h3>
      </@modal.header>
      <@modal.body>
        <p>
          Please check that the outcomes are correct and that the reasons given are appropriate.
          In particular, please ensure that accurate minutes have been recorded that contain sufficient detail for any future appeals or OIA investigations.
        </p>
        <@f.hidden path="approve" value="true"/>
      </@modal.body>
    </#if>
    <@modal.footer>
      <button type="submit" class="btn btn-primary">Confirm</button>
      <a class="btn btn-default dirty-check-ignore" href="<#if fromPanel && submission.panel??><@routes.mitcircs.reviewSubmissionPanel submission /><#else><@routes.mitcircs.reviewSubmission submission /></#if>">Cancel</a>
    </@modal.footer>
  </@f.form>
</#escape>