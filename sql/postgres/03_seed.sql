-- Containment sites (5 записей)
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES
    ('Site-17',  'United States, classified',     50, 4),
    ('Site-19',  'United States, undisclosed',    80, 5),
    ('Area-14',  'Nevada, desert region',         30, 5),
    ('Site-66',  'Eastern Europe',                40, 3),
    ('Bio-Site-66','Bio-research wing, Site-66',  20, 4);

-- SCP objects (5 записей)
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES
    ('SCP-173', 'The Sculpture',           'Euclid', 'Hostile concrete statue. Cannot move while observed.',           '1993-06-12'),
    ('SCP-682', 'Hard-to-Destroy Reptile', 'Keter',  'Highly adaptive reptilian entity. Multiple termination attempts.','1965-09-04'),
    ('SCP-049', 'Plague Doctor',           'Euclid', 'Humanoid in plague doctor attire. Believes it cures "the pestilence".','1935-03-19'),
    ('SCP-096', 'The Shy Guy',             'Euclid', 'Humanoid that responds violently to facial observation.',         '1989-07-22'),
    ('SCP-914', 'The Clockworks',          'Safe',   'Mechanical device transforming inputs based on setting.',          '1962-11-08');

-- Personnel (6 записей)
INSERT INTO personnel (full_name, position, clearance_level, base_site_id) VALUES
    ('Dr. Alto Clef',          'Senior Researcher',         5, 2),
    ('Dr. Simon Glass',        'Head of Psychology',        4, 2),
    ('Agent Daniel Navarro',   'Field Operative',           3, 1),
    ('Dr. Elena Volkov',       'Containment Specialist',    3, 4),
    ('Captain Marcus Hayes',   'Security Director',         4, 3),
    ('Researcher Yuki Tanaka', 'Junior Researcher',         2, 5);

-- MTF teams (5 записей)
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES
    ('MTF Alpha-1',     'Red Right Hand — direct O5 enforcement',  2),
    ('MTF Epsilon-11',  'Nine-Tailed Fox — recontainment',         2),
    ('MTF Mu-4',        'Debuggers — informational anomalies',     1),
    ('MTF Nu-7',        'Hammer Down — heavy assault',             3),
    ('MTF Beta-7',      'Maz Hatters — chemical/biohazard',        5);

-- MTF members (6 записей)
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES
    (1, 1, '2020-01-15'),
    (2, 3, '2021-03-20'),
    (2, 5, '2019-08-01'),
    (3, 2, '2022-05-12'),
    (4, 5, '2020-11-30'),
    (5, 4, '2023-02-14');

-- Containment history (6 записей)
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES
    (1, 2, '1993-07-01', NULL),
    (2, 3, '1965-10-15', '2010-04-22'),
    (2, 2, '2010-04-22', NULL),
    (3, 4, '1935-04-01', NULL),
    (4, 2, '1989-08-10', NULL),
    (5, 1, '1962-11-15', NULL);

-- Incidents (5 записей)
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES
    ('2023-04-12 03:22:00', 1, 2, 2, 4, 'Containment breach during routine maintenance. 3 personnel casualties before recontainment.'),
    ('2023-07-08 14:15:00', 2, 2, 4, 5, 'Subject attempted breach. Heavy weapons deployed.'),
    ('2024-01-23 09:00:00', 3, 4, NULL, 2, 'Subject expressed agitation during interview. No physical incident.'),
    ('2024-03-15 22:40:00', 4, 2, 2, 3, 'Visual contact established with junior staff. One casualty.'),
    ('2024-09-30 11:00:00', 5, 1, NULL, 1, 'Setting dial malfunction during routine experiment. No anomalous effect.');

-- Procedure revisions (5 записей)
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES
    (1, 1, '1993-07-15', 'Subject must be kept under direct observation by minimum 2 personnel at all times. Blinking shifts coordinated.', 1),
    (1, 2, '2010-06-22', 'Revised: minimum 3 personnel with overlapping observation windows. Camera redundancy required.', 1),
    (2, 1, '1965-10-20', 'Subject contained in reinforced cell. Standard armaments insufficient.', 2),
    (3, 1, '1935-04-05', 'Subject is cooperative within research contexts. Maintain dialogue protocols.', 2),
    (5, 1, '1962-12-01', 'Device operates without anomalous effect on operator. Refine setting documentation.', 1);

-- Users (5 записей: 1 O5 + 4 researchers с разным clearance)
-- ВАЖНО: hash для admin/scp-foundation. Реальное значение получить из PasswordHasher.main()
-- после Batch 9. Сейчас placeholder.
INSERT INTO users (personnel_id, login, password_hash, salt, role) VALUES
    (1, 'admin',    'REPLACE_AFTER_BATCH_9_FOR_admin_____________________________0000', 'salt_admin_REPLACE_32_chars_____', 'O5'),
    (2, 'sglass',   'REPLACE_AFTER_BATCH_9_FOR_sglass____________________________0000', 'salt_sglass_REPLACE_32_chars____', 'RESEARCHER'),
    (3, 'dnavarro', 'REPLACE_AFTER_BATCH_9_FOR_dnavarro__________________________0000', 'salt_dnavarro_REPLACE_32_chars__', 'RESEARCHER'),
    (4, 'evolkov',  'REPLACE_AFTER_BATCH_9_FOR_evolkov___________________________0000', 'salt_evolkov_REPLACE_32_chars___', 'RESEARCHER'),
    (6, 'ytanaka',  'REPLACE_AFTER_BATCH_9_FOR_ytanaka___________________________0000', 'salt_ytanaka_REPLACE_32_chars___', 'RESEARCHER');
