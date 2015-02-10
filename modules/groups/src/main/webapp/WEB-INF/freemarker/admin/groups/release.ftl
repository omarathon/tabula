<#escape x as x?html>
    <#assign smallGroupSet = releaseGroupSetCommand.singleGroupToPublish/>
    <#assign submitAction><@routes.releaseset smallGroupSet /></#assign>
    <#if smallGroupSet.releasedToStudents >
        <#assign studentReadonly="disabled"/>
    </#if>


    <div class="modal-header">
        <h3>Publish</h3>
    </div>

    <#-- Have to manually remove the 20px margin from the form (added by default bootstrap form styles)-->
    <@f.form method="post" action="${submitAction}" commandName="releaseGroupSetCommand" cssClass="form-horizonatal form-tiny" style="margin-bottom:0">

    <div class="modal-body">
        <p>Publish allocations for ${smallGroupSet.name} so they are shown in Tabula to:</p>
        <@form.row "notifyStudents">
            <label class='checkbox ${smallGroupSet.releasedToStudents?string("disabled use-tooltip' title='Already published'","'") }>
                <@f.checkbox path="notifyStudents" disabled=smallGroupSet.releasedToStudents />Students
            </label>
        </@form.row>
        <@form.row "notifyTutors" >
            <label class="checkbox ${smallGroupSet.releasedToTutors?string('disabled','')}">
                <@f.checkbox path="notifyTutors" disabled=smallGroupSet.releasedToTutors />Tutors
            </label>
        </@form.row>
        <hr>
        <@form.row "sendEmail">
        	<@form.label checkbox=true>
				<@f.checkbox path="sendEmail" />
				Send an email about this and any future changes to group allocation
        	</@form.label>
        </@form.row>
    </div>
    <div class="modal-footer">
    <input class="btn btn-info" type="submit" value="Publish"> <a class="btn cancel-link" data-dismiss="modal" href="#">Cancel</a>
    </div>
    </@f.form>
</#escape>