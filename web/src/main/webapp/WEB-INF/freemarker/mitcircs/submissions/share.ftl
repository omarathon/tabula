<#import "*/permissions_macros.ftl" as pm />

<#escape x as x?html>
  <div class="deptheader">
    <h1>Share mitigating circumstances submission MIT-${submission.key}</h1>
  </div>

  <p>You can share your submission with anyone at the University below. If you share the submission
  with someone, they'll get an email with a link to your submission and will be able to see it until
  the submission has been submitted or withdrawn. Once you submit or withdraw your submission, the
  people you've shared it with won't be able to see it any more.</p>

  <#assign perms_url><@routes.mitcircs.shareSubmission submission /></#assign>

  <@pm.alerts "addCommand" "MIT-${submission.key}" users role />
  <@pm.roleTable perms_url "mitcircs-shared-table" submission "MitigatingCircumstancesViewerRoleDefinition" "shared user" true />

  <div class="submit-buttons">
    <a class="btn btn-default" href="<@routes.mitcircs.viewSubmission submission/>">Done</a>
  </div>
</#escape>