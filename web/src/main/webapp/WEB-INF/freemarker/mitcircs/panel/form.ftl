<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/mitcircs_components.ftl" as components />

<#escape x as x?html>

  <div id="profile-modal" class="modal fade profile-subset"></div>

  <#function route_function dept>
    <#local result><@routes.mitcircs.adminhome dept /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader "Create a mitigating circumstances panel" route_function "for" />

  <@f.form method="POST" modelAttribute="createCommand" class="mitcircs-panel-form dirty-check double-submit-protection" enctype="multipart/form-data">

    <@bs3form.labelled_form_group path="name" labelText="Name">
      <@f.input path="name" cssClass="form-control" />
    </@bs3form.labelled_form_group>

    <div class="row">
      <div class="col-xs-4">
        <@bs3form.labelled_form_group path="date" labelText="Date of panel">
          <div class="input-group">
            <@f.input path="date" autocomplete="off" cssClass="form-control date-picker" />
            <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
          </div>
        </@bs3form.labelled_form_group>
      </div>
      <div class="col-xs-4">
        <@bs3form.labelled_form_group path="start" labelText="Start time of panel">
          <div class="input-group">
            <@f.input path="start" autocomplete="off" cssClass="form-control time-picker" />
            <span class="input-group-addon"><i class="fa fa-clock-o"></i></span>
          </div>
        </@bs3form.labelled_form_group>
      </div>
      <div class="col-xs-4">
        <@bs3form.labelled_form_group path="end" labelText="End time of panel">
          <div class="input-group">
            <@f.input path="end" autocomplete="off" cssClass="form-control time-picker" />
            <span class="input-group-addon"><i class="fa fa-clock-o"></i></span>
          </div>
        </@bs3form.labelled_form_group>
      </div>
    </div>

    <@bs3form.labelled_form_group path="location" labelText="Location">
      <@f.hidden path="locationId" />
      <@f.input path="location" cssClass="form-control location-picker" />
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="chair" labelText="Panel chair">
      <@bs3form.flexipicker path="chair" placeholder="User name" list=false object=false multiple=false />
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="secretary" labelText="Panel secretary">
      <@bs3form.flexipicker path="secretary" placeholder="User name" list=false object=false multiple=false />
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="members" labelText="Panel members">
      <@bs3form.flexipicker path="members" placeholder="User name" list=true object=false multiple=true auto_multiple=false />
    </@bs3form.labelled_form_group>

    <#if hasPanel?has_content>
      <@bs3form.labelled_form_group path="" labelText="Submissions being moved to this panel">
        <p>The following submissions have already been added to another panel. They will be moved to this panel.</p>
        <@components.submissionTable submissions=hasPanel panel=true />
      </@bs3form.labelled_form_group>
    </#if>

    <@bs3form.labelled_form_group path="submissions" labelText="Submissions being added to this panel">
      <@components.submissionTable noPanel />
    </@bs3form.labelled_form_group>

    <div class="fix-footer">
      <button type="submit" class="btn btn-primary" name="submit">Create panel</button>
      <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.adminhome department academicYear/>">Cancel</a>
    </div>

  </@f.form>
</#escape>