INSERT INTO STUDENTRELATIONSHIPTYPE (ID, URLPART, DESCRIPTION, AGENTROLE, STUDENTROLE, DEFAULTSOURCE, DEFAULTDISPLAY, EXPECTED_UG, EXPECTED_PGT, EXPECTED_PGR,
                                     SORT_ORDER)
VALUES ('personalTutor', 'tutor', 'Personal Tutor', 'personal tutor', 'personal tutee', 'local', true, true, true, false, 0);

INSERT INTO STUDENTRELATIONSHIPTYPE (ID, URLPART, DESCRIPTION, AGENTROLE, STUDENTROLE, DEFAULTSOURCE, DEFAULTDISPLAY, EXPECTED_UG, EXPECTED_PGT, EXPECTED_PGR,
                                     SORT_ORDER)
VALUES ('supervisor', 'supervisor', 'Research Supervisor', 'supervisor', 'supervisee', 'sits', true, false, false, true, 1);