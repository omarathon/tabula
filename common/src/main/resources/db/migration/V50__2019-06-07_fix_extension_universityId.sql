update extension e
  set universityid = m.universityid
from member m
  where e.userid = m.userid and e.universityid != m.universityid;
