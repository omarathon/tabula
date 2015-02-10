<#escape x as x?html>
<h1>Publish groups in ${department.name}</h1>
<#if info.requestParameters.batchReleaseSuccess??>
<div class="alert alert-success">
    These small group allocations have been published
</div>
</#if>
    <@f.form method="post" action="" commandName="moduleList" cssClass="form-horizonatal form-tiny">
    	<p>Publish these groups so they are shown in Tabula to:</p>
        <@form.row "notifyStudents">
			<label class="checkbox">
				<@f.checkbox path="notifyStudents"/>Students
			</label>
        </@form.row>
        <@form.row "notifyTutors" >
			<label class="checkbox">
				<@f.checkbox path="notifyTutors"/>Tutors
			</label>
        </@form.row>
		<hr>
		<@form.row "sendEmail">
			<@form.label checkbox=true>
				<@f.checkbox path="sendEmail" />
				Send an email about this and any future changes to group allocation
			</@form.label>
		</@form.row>

        <div class="control-group">
            <input class="btn btn-info" type="submit" value="Publish">
        </div>

    <div id="scroll-container">
    <div id="tooltip-container"></div>
    <table id="modules-table" class="table table-bordered table-striped sortable-table">

        <thead>
        <tr>
            <th><@form.selector_check_all /></th>
            <th class="sortable">Module</th>
        </tr>
        </thead>
        <tbody >
            <#list modules as module>
            <tr ${module.hasUnreleasedGroupSets(academicYear)?string("","class='use-tooltip' title='Groups for this module have already been published'")} data-container="#tooltip-container">
                <td>
                   <@f.checkbox
                   class=module.hasUnreleasedGroupSets(academicYear)?string('collection-checkbox','')
                   path="checkedModules"
                   disabled=(!module.hasUnreleasedGroupSets(academicYear))
                   value=module.code/>
                </td>
                <td>
                    <span class="${module.hasUnreleasedGroupSets(academicYear)?string('','muted')}">
                        <@fmt.module_name module false />
                    </span>
                </td>
            </tr>
            </#list>

        </tbody>
    </table>
    </div>


    </@f.form>
</#escape>
<@script "/static/js/sortable-table.js" />
<script type="text/javascript">
    jQuery("#modules-table").bigList({});
</script>