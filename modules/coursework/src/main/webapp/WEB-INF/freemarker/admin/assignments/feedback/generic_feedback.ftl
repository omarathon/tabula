<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<div class="form onlineFeedback">
	<@f.form cssClass="form-horizontal" method="post" commandName="command" action="generic">

		<@form.row>
			<@form.label path="genericFeedback">Generic feedback</@form.label>
			<@form.field>
				<@f.textarea path="genericFeedback" cssClass="" />
			</@form.field>
		</@form.row>

		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Save">
			<a class="btn cancel-feedback" href="">Discard</a>
		</div>
	</@f.form>
</div>
