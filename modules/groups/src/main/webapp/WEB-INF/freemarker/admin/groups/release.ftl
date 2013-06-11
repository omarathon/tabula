<#escape x as x?html>
    <#assign smallGroupSet = releaseGroupSetCommand.groupToPublish/>
    <#assign submitAction><@routes.releaseset smallGroupSet /></#assign>
    <#if smallGroupSet.releasedToStudents >
        <#assign studentReadonly="disabled"/>
    </#if>


    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
        <h3>Notify</h3>
    </div>

    <@f.form method="post" action="${submitAction}" commandName="releaseGroupSetCommand" cssClass="form-horizonatal form-tiny">

    <div class="modal-body">
        <p>Notify these people via email that ${smallGroupSet.name} ${smallGroupSet.format.description}
            allocations for ${smallGroupSet.module.code} are ready to view
            in Tabula</p>
        <@form.row "notifyStudents">
            <label class="checkbox ${smallGroupSet.releasedToStudents?string('disabled','')}">
                <@f.checkbox path="notifyStudents" disabled="${smallGroupSet.releasedToStudents?string}"/>Students
            </label>
        </@form.row>
        <@form.row "notifyTutors" >
            <label class="checkbox ${smallGroupSet.releasedToTutors?string('disabled','')}">
                <@f.checkbox path="notifyTutors" disabled="${smallGroupSet.releasedToTutors?string}"/>Tutors
            </label>
        </@form.row>
    </div>
    <div class="modal-footer">
    <input class="btn btn-info" type="submit" value="Notify"> <a class="btn cancel-link" data-dismiss="modal" href="#">Cancel</a>
    </div>
    </@f.form>
</#escape>