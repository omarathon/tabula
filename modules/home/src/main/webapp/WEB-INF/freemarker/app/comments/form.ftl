<#compress>
<#escape x as x?html>

<@f.form action="/app/tell-us" method="post" commandName="appCommentCommand" id="app-comment-form" cssClass="form-horizontal">
	
	<p>
	Do you have a comment, complaint or suggestion related to this application? Let us know here.
	<#if (appCommentCommand.componentName!"") == 'courses'>
		Note that if you have a question about your course material or want to talk about some feedback/marks
		you received, you should talk to the person setting your coursework.
	</#if>
	(If you are looking for instructions on how to use Tabula, you might like to look at <a href="http://warwick.ac.uk/tabula/faqs/" target="_blank">these FAQs</a>)
	</p>
	
	<p>
	We've filled in some information below about you and your computer in order to
	help diagnose any problems you might be reporting; feel free to amend or remove any of it.
	</p>

	<#-- DRY -->
	<#macro comment_input path title>
		<@form.labelled_row path title>
			<@f.input cssClass="text" path=path id="app-comment-${path}" />
		</@form.labelled_row>
	</#macro>
	
	<h4>About you</h4>
	<@comment_input "name" "Your name" />
	<@comment_input "email" "Your email" />
	<@comment_input "usercode" "Usercode" />
	
	<h4 class="browser-info-heading">About your browser</h4>
	<div class="browser-info">
		<@comment_input "currentPage" "The page you're on" />
		<@comment_input "browser" "Browser" />
		<@comment_input "os" "Operating System" />
		<@comment_input "resolution" "Screen size" />
		<@comment_input "ipAddress" "IP Address" />
	</div>

	<h4>Your message</h4>
	<@f.errors path="message" cssClass="error" />	
	<@f.textarea path="message" id="app-comment-message" />
	
	<#--
	<div>
	<#if user.loggedIn>
	<@f.checkbox path="pleaseRespond" id="app-comment-response" />
	<@f.label for="app-comment-response">I would like a response, please.</@f.label>
	<#else>
	<div class="disabled-zone">
	<input type="checkbox" disabled> If you would like a response, you'll need to sign in first.
	</div>
	</#if>
	</div>
	-->
	
	<div class="submit-buttons"><input class="btn btn-primary" type="submit" value="Send"></div>
</@f.form>

<script>

jQuery('#app-comment-form').submit(function(event){
	
});

</script>

</#escape>
</#compress>
