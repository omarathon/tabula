<#escape x as x?html>
    <#assign smallGroupSet = releaseGroupSetCommand.groupToPublish/>
    <#assign submitAction><@routes.releaseset smallGroupSet /></#assign>
    <div class="modal-header">
        <h3>Notify</h3>
    </div>
    <@f.form method="post" action="${submitAction}" commandName="releaseGroupSetCommand" cssClass="form-vertical">

    <div class="modal-body">
        <p>Notify these people via email that ${smallGroupSet.name} ${smallGroupSet.format.description}
            allocations for ${smallGroupSet.module.code} are ready to view
            in Tabula</p>

        <@form.row "notifyStudents">
            <label class="checkbox">
                <@f.checkbox path="notifyStudents" />Students
            </label>
        </@form.row>
        <@form.row "notifyTutors" >
            <label class="checkbox">
                <@f.checkbox path="notifyTutors" />Tutors
            </label>
        </@form.row>
    </div>
    <div class="modal-footer">
    <input class="btn btn-info" type="submit" value="Notify"> <a class="btn cancel-link" href="#">Cancel</a>
    </div>
    </@f.form>
</#escape>