<#compress>
<#escape x as x?html>

<@f.form action="/app/tell-us" method="post" commandName="appCommentCommand" id="app-comment-form">
	
	<p>
	Do you have a comment, complaint or suggestion related to this application? Let us know here.
	Note that if you have a question about your course material or want to talk about some feedback/marks
	you received, you should talk to the person setting your coursework.
	</p>
	
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
	
	<div class="submit-buttons actions"><input type="submit" value="Send"></div>
</@f.form>

<script>

jQuery('#app-comment-form').submit(function(event){
	
});

</script>

</#escape>
</#compress>
