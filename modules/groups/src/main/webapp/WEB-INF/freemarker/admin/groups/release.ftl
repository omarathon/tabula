<#escape x as x?html>
    <#assign smallGroupSet = releaseGroupSetCommand.singleGroupToPublish/>
    <#assign submitAction><@routes.releaseset smallGroupSet /></#assign>
    <#if smallGroupSet.releasedToStudents >
        <#assign studentReadonly="disabled"/>
    </#if>


    <div class="modal-header">
        <h3>Notify</h3>
    </div>

    <#-- Have to manually remove the 20px margin from the form (added by default bootstrap form styles)-->
    <@f.form method="post" action="${submitAction}" commandName="releaseGroupSetCommand" cssClass="form-horizonatal form-tiny" style="margin-bottom:0">

    <div class="modal-body">
        <p>Notify these people via email that ${smallGroupSet.name}
            allocations for ${smallGroupSet.module.code} are ready to view
            in Tabula</p>
        <@form.row "notifyStudents">
            <label class='checkbox ${smallGroupSet.releasedToStudents?string("disabled use-tooltip' title='Already notified'","'") }>
                <@f.checkbox path="notifyStudents" disabled="${smallGroupSet.releasedToStudents?string}"/>Students
            </label>
        </@form.row>
        <@form.row "notifyTutors" >
            <label class="checkbox ${smallGroupSet.releasedToTutors?string('disabled','')}">
                <@f.checkbox path="notifyTutors" disabled="${smallGroupSet.releasedToTutors?string}"/>Tutors
            </label>
        </@form.row>
        <p>They will automatically be notified of any further changes made to these groups</p>
    </div>
    <div class="modal-footer">
    <input class="btn btn-info" type="submit" value="Notify" data-update-target="#groupset-container-${smallGroupSet.id}"> <a class="btn cancel-link" data-dismiss="modal" href="#">Cancel</a>
    </div>
    </@f.form>
</#escape>