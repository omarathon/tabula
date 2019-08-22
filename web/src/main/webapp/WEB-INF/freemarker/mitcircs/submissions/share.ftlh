<#import "*/permissions_macros.ftl" as pm />
<#import "*/modal_macros.ftlh" as modal />
<#import "*/mitcircs_components.ftlh" as components />

<div class="deptheader">
  <h1>Share mitigating circumstances submission MIT-${submission.key}</h1>
</div>

<p>You can share your submission with any member of staff at Warwick. Sharing your submission, will trigger an email with a link to your submission to the member of staff. They will be able to see this submission until you submit or withdraw it.</p>
<p>You can't share your submission with other students.</p>
<p>By sharing your submission you confirm that you have read the <a role="button" data-toggle="modal" data-target="#privacyStatement">privacy statement</a> and understand how the information you provide will be used.</p>

<@modal.modal id="privacyStatement">
  <@modal.wrapper>
    <@modal.header>
      <h6 class="modal-title">Privacy statement</h6>
    </@modal.header>
    <@modal.body>
      <@components.privacyStatement />
    </@modal.body>
    <@modal.footer>
      <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
    </@modal.footer>
  </@modal.wrapper>
</@modal.modal>

<#assign perms_url><@routes.mitcircs.shareSubmission submission /></#assign>

<@pm.alerts "addCommand" "MIT-${submission.key}" users role />
<@pm.roleTable perms_url?markup_string "mitcircs-shared-table" submission "MitigatingCircumstancesViewerRoleDefinition" "shared user" true />

<div class="submit-buttons">
  <a class="btn btn-default" href="<@routes.mitcircs.viewSubmission submission/>">Done</a>
</div>
