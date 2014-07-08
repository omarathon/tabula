<#import "../membernote/member_notes_macros.ftl" as member_notes_macros />

<section id="membernote-details" class="clearfix membernote">
	<h4>Administrative notes</h4>

	<#if memberNotes??><@member_notes_macros.member_note_list memberNotes /></#if>
</section>


