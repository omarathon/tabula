<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/mitcircs_components.ftl" as components />

<#escape x as x?html>
  <div class="deptheader">
    <h1>Edit mitigating circumstances panel</h1>
  </div>

  <@f.form method="POST" modelAttribute="command" class="mitcircs-panel-form dirty-check double-submit-protection">
    <#include "_fields.ftl" />

    <@bs3form.labelled_form_group path="submissions" labelText="Submissions in this panel">
      <@components.submissionTable command.submissions />
    </@bs3form.labelled_form_group>

    <div class="submit-buttons">
      <button type="submit" class="btn btn-primary" name="submit">Edit panel</button>
      <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.viewPanel panel/>">Cancel</a>
    </div>
  </@f.form>
</#escape>