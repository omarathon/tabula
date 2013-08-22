<#escape x as x?html>

<#-- This might end up being AJAXically loaded into the top of the main screen -->

<#assign heading>
	New monitoring point set for ${command.route.code?upper_case} ${command.route.name}
</#assign>

<#if modal??>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h2>${heading}</h2>
	</div>
<#else>
	<h1>${heading}</h1>
</#if>

<#if modal??>
	<div class="modal-body">
</#if>

<#assign action><@url page="/manage/${command.route.department.code}/sets/add" /></#assign>

<@f.form id="newMonitoringPointSet" action="${action}" method="POST" commandName="command" class="form-horizontal">
	<input type="hidden" value="${command.route.code}" name="route" />
	<@f.errors path="route" cssClass="error" />

	<@form.labelled_row "year" "Year of study">
		<#assign validYears = command.validYears>
		<#if validYears?size == 0>
			You cannot add any more sets to this route
		<#else>
			<@f.select path="year">
				<#list validYears as year>
					<#assign yearValue = "">
					<#if year != "All"><#assign yearValue = year></#if>
            		<@f.option value="${yearValue}" label="${year}" />
            	</#list>
            </@f.select>
		</#if>

	</@form.labelled_row>

	<@form.labelled_row "academicYear" "Academic year">
    		<@f.select path="academicYear">
    			<@f.option value="${command.academicYear.previous.toString}" />
                <@f.option value="${command.academicYear.toString}" />
                <@f.option value="${command.academicYear.next.toString}" />
            </@f.select>
    	</@form.labelled_row>

	<@form.labelled_row "templateName" "Template name">
		<@f.select path="templateName">
			<@f.option value="1st year Undergraduate" />
			<@f.option value="2nd year Undergraduate" />
			<@f.option value="3rd year Undergraduate" />
			<@f.option value="4th year Undergraduate" />
			<@f.option value="Postgraduate research" />
			<@f.option value="Postgraduate taught" />
			<@f.option value="" label="Other&hellip;" htmlEscape="false" />
		</@f.select>
		<@f.input path="customTemplateName" />
	</@form.labelled_row>

	<script>
		jQuery(function($){
			$('form#newMonitoringPointSet')
				.find('select[name="templateName"]').on('change',function(){
					if($(this).val().length === 0) {
						$(this).hide().siblings('input[name="customTemplateName"]').show().focus();
					}
				}).end()
				.find('input[name="customTemplateName"]').hide();
		});
	</script>

	<#if modal??>
    	<input type="hidden" name="modal" value="true" />
    <#else>
		<div class="submit-buttons">
			<button class="btn btn-primary btn-large">Create</button>
		</div>
	</#if>
</@f.form>

<#if modal??>
	</div>
	<div class="modal-footer">
		<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Creating&hellip;">
	     	Create
	    </button>
	    <button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
	</div>
</#if>

</#escape>