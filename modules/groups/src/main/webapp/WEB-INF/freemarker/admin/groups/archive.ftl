<#escape x as x?html>
	<#assign submitAction><@routes.archiveset smallGroupSet /></#assign>
	<@f.form method="post" action="${submitAction}" commandName="archiveSmallGroupSetCommand" cssClass="form-vertical">		
		<#if smallGroupSet.archived>
		
			<h3>Unarchive these groups</h3>
			
			<p>You should only unarchive groups that you've archived by mistake. 
			If you want to do anything new, you should create new groups.</p>
			
			<input type="hidden" name="unarchive" value="true" />
			<input class="btn" type="submit" value="Unarchive"> <a class="btn cancel-link" href="#">Cancel</a>
		
		<#else>
		
			<h3>Archive these groups</h3>
			
			<p>Archiving groups will hide them from most lists of things. Students
			will still be able to access their group allocations from archived
			groups.</p>
			
			<input class="btn" type="submit" value="Archive"> <a class="btn cancel-link" href="#">Cancel</a>
		
		</#if>
	</@f.form>
</#escape>