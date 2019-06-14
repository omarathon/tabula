<#escape x as x?html>
  <div class="deptheader">
    <h1>Withdraw mitigating circumstances submission MIT-${submission.key}</h1>
  </div>

  <p>If you withdraw this mitigating circumstances submission, it will not be considered by the department
  as a request for mitigation. You can re-open the submission later if circumstances change and you decide
  to submit a request for mitigation again.</p>

  <@f.form method="POST" modelAttribute="command" class="double-submit-protection">
    <div class="submit-buttons">
      <button type="submit" class="btn btn-primary">Withdraw submission</button>
      <a class="btn btn-default" href="<@routes.mitcircs.viewSubmission submission/>">Cancel</a>
    </div>
  </@f.form>
</#escape>