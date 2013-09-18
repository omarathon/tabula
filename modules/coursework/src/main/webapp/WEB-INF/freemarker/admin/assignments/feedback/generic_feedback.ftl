<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<div class="form onlineFeedback">
	<@f.form cssClass="form-horizontal" method="post" commandName="command" action="generic">
		<div>
			<@f.textarea path="genericFeedback" cssClass="span9" rows="6"/>
		</div>
		<div class="help-block">
			The following comments will be released to all students along with there individual feedback.
		</div>
		<div class="submit-buttons">
			<input class="before-save btn btn-primary" type="submit" value="Save">
			<a class="before-save btn cancel-feedback" href="">Discard</a>
			<a class="saving btn btn-primary disabled" style="display:none" onclick="return false;" href="">
				<i class="icon-spinner icon-spin"></i> Saving
			</a>
			<span class="saved label label-success" style="display:none">Changes saved</span>
		</div>
	</@f.form>
</div>
