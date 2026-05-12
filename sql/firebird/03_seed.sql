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

-- Хеши SHA-256(salt || password) сгенерированы через PasswordHasher (Batch 9).
-- Пароли: admin/scp-foundation, sglass/euclid-1989, dnavarro/alpha-bravo,
--         evolkov/containment-66, ytanaka/research-2024.
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (1, 'admin',    '162fd61c7339ad5e64f3fe16a70cd74efdd8d85dc4ec2045b2c62b15134e5dcd', '8be3f19ea48185fc6e97bb0a91129747', 'O5');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (2, 'sglass',   '6b0baa7b344774f0174258e3e7d9df62bc6898b9738b2b61e9657df7b366f922', 'a154008cee3c47e0d0ee4eab4f5adca6', 'RESEARCHER');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (3, 'dnavarro', '24bd38e2d4cf775c27572bce26eb6f3f9cd096c81a3b16a621c58680cc4e8013', '97bf57a8bba5436506b9b0f97c402c68', 'RESEARCHER');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (4, 'evolkov',  '4e91f95bdf7d90826ac77ec541c3713c672e97a87668123436452aee83d7eb08', 'b03103ac7def653127ac9db77ccaa6f5', 'RESEARCHER');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (6, 'ytanaka',  'd4ca647a49ecc7eaf8badea5ae360eaaf1959809fa2ecd19c5fd42b897d2a0d8', '0756205e08b188dbe8724c2cd1d907f7', 'RESEARCHER');

COMMIT;
