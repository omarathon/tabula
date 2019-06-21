<#import "*/mitcircs_components.ftl" as components />

<#escape x as x?html>
  <#function route_function dept>
    <#local result><@routes.mitcircs.createPanel dept academicYear /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader "Create a mitigating circumstances panel" route_function "for" />

  <@f.form method="POST" modelAttribute="createCommand" class="mitcircs-panel-form dirty-check double-submit-protection">
    <#include "_fields.ftl" />

    <#if hasPanel?has_content>
      <@bs3form.labelled_form_group path="" labelText="Submissions being moved to this panel">
        <p>The following submissions have already been added to another panel. They will be moved to this panel.</p>
        <@components.submissionTable submissions=hasPanel panel=true />
      </@bs3form.labelled_form_group>
    </#if>

    <@bs3form.labelled_form_group path="submissions" labelText="Submissions being added to this panel">
      <@components.submissionTable noPanel />
    </@bs3form.labelled_form_group>

    <div class="submit-buttons">
      <button type="submit" name="submit" class="btn btn-primary">Create panel</button>
      <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.adminhome department academicYear/>">Cancel</a>
    </div>
  </@f.form>
</#escape>