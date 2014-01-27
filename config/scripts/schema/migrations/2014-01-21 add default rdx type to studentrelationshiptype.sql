ALTER TABLE STUDENTRELATIONSHIPTYPE
  ADD (DEFAULTRDXTYPE NVARCHAR2(12)
);

update studentrelationshiptype set defaultrdxtype = 'SUP' where id = 'supervisor';

update studentrelationshiptype set defaultrdxtype = 'WASUP' where id = 'diss-supervisor';

