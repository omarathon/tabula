<#escape x as x?html>
  <div class="deptheader">
    <h1>Re-open mitigating circumstances submission MIT-${submission.key}</h1>
  </div>

  <p>This mitigating circumstances submission is currently withdrawn and will not be considered for mitigation.
  You can re-open the submission to make changes and then to submit it.</p>

  <@f.form method="POST" modelAttribute="command" class="double-submit-protection">
    <div class="submit-buttons">
      <button type="submit" class="btn btn-primary">Re-open submission</button>
      <a class="btn btn-default" href="<@routes.mitcircs.viewSubmission submission/>">Cancel</a>
    </div>
  </@f.form>
</#escape>