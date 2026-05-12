INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Site-17',  'United States, classified',     50, 4);
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Site-19',  'United States, undisclosed',    80, 5);
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Area-14',  'Nevada, desert region',         30, 5);
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Site-66',  'Eastern Europe',                40, 3);
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Bio-Site-66','Bio-research wing, Site-66',  20, 4);

INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-173', 'The Sculpture',           'Euclid', 'Hostile concrete statue. Cannot move while observed.',           '1993-06-12');
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-682', 'Hard-to-Destroy Reptile', 'Keter',  'Highly adaptive reptilian entity.',                              '1965-09-04');
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-049', 'Plague Doctor',           'Euclid', 'Humanoid in plague doctor attire.',                               '1935-03-19');
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-096', 'The Shy Guy',             'Euclid', 'Responds violently to facial observation.',                       '1989-07-22');
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-914', 'The Clockworks',          'Safe',   'Mechanical device transforming inputs based on setting.',         '1962-11-08');

INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Dr. Alto Clef',          'Senior Researcher',         5, 2);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Dr. Simon Glass',        'Head of Psychology',        4, 2);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Agent Daniel Navarro',   'Field Operative',           3, 1);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Dr. Elena Volkov',       'Containment Specialist',    3, 4);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Captain Marcus Hayes',   'Security Director',         4, 3);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Researcher Yuki Tanaka', 'Junior Researcher',         2, 5);

INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Alpha-1',     'Red Right Hand',          2);
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Epsilon-11',  'Nine-Tailed Fox',         2);
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Mu-4',        'Debuggers',               1);
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Nu-7',        'Hammer Down',             3);
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Beta-7',      'Maz Hatters',             5);

INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (1, 1, '2020-01-15');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (2, 3, '2021-03-20');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (2, 5, '2019-08-01');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (3, 2, '2022-05-12');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (4, 5, '2020-11-30');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (5, 4, '2023-02-14');

INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (1, 2, '1993-07-01', NULL);
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (2, 3, '1965-10-15', '2010-04-22');
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (2, 2, '2010-04-22', NULL);
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (3, 4, '1935-04-01', NULL);
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (4, 2, '1989-08-10', NULL);
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (5, 1, '1962-11-15', NULL);

INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2023-04-12 03:22:00', 1, 2, 2, 4, 'Containment breach. 3 casualties.');
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2023-07-08 14:15:00', 2, 2, 4, 5, 'Subject attempted breach.');
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2024-01-23 09:00:00', 3, 4, NULL, 2, 'Subject expressed agitation during interview.');
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2024-03-15 22:40:00', 4, 2, 2, 3, 'Visual contact with junior staff. One casualty.');
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2024-09-30 11:00:00', 5, 1, NULL, 1, 'Setting dial malfunction.');

INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (1, 1, '1993-07-15', 'Min. 2 personnel observation.', 1);
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (1, 2, '2010-06-22', 'Min. 3 personnel + camera redundancy.', 1);
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (2, 1, '1965-10-20', 'Reinforced cell required.', 2);
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (3, 1, '1935-04-05', 'Dialogue protocols.', 2);
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (5, 1, '1962-12-01', 'Refine setting documentation.', 1);

-- Хеши обновить после Batch 9
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (1, 'admin',    'REPLACE_AFTER_BATCH_9_FOR_admin_____________________________0000', 'salt_admin_REPLACE_32_chars_____', 'O5');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (2, 'sglass',   'REPLACE_AFTER_BATCH_9_FOR_sglass____________________________0000', 'salt_sglass_REPLACE_32_chars____', 'RESEARCHER');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (3, 'dnavarro', 'REPLACE_AFTER_BATCH_9_FOR_dnavarro__________________________0000', 'salt_dnavarro_REPLACE_32_chars__', 'RESEARCHER');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (4, 'evolkov',  'REPLACE_AFTER_BATCH_9_FOR_evolkov___________________________0000', 'salt_evolkov_REPLACE_32_chars___', 'RESEARCHER');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (6, 'ytanaka',  'REPLACE_AFTER_BATCH_9_FOR_ytanaka___________________________0000', 'salt_ytanaka_REPLACE_32_chars___', 'RESEARCHER');

COMMIT;
