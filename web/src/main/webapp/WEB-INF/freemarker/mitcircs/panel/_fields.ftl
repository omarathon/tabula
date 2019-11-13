<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

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
  <@bs3form.flexipicker path="chair" placeholder="User name" staffOnly="true" list=false object=false multiple=false />
</@bs3form.labelled_form_group>

<@bs3form.labelled_form_group path="secretary" labelText="Panel secretary">
  <@bs3form.flexipicker path="secretary" placeholder="User name" staffOnly="true" list=false object=false multiple=false />
</@bs3form.labelled_form_group>

<@bs3form.labelled_form_group path="members" labelText="Panel members">
  <@bs3form.flexipicker path="members" placeholder="User name" staffOnly="true" list=true object=false multiple=true auto_multiple=false />
</@bs3form.labelled_form_group>