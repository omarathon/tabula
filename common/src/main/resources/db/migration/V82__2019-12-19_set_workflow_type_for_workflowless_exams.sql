update assignment
set workflow_category = 'N'
where id in (select id from exam)
  and cm2assignment
  and cm2_workflow_id is null
  and workflow_category = 'S';
