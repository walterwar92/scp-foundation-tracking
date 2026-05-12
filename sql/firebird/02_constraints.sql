-- CHECK constraints
ALTER TABLE scp_objects
    ADD CONSTRAINT chk_object_class CHECK (object_class IN ('Safe', 'Euclid', 'Keter'));

ALTER TABLE containment_sites
    ADD CONSTRAINT chk_security_level CHECK (security_level BETWEEN 1 AND 5);

ALTER TABLE containment_history
    ADD CONSTRAINT chk_history_dates CHECK (moved_out IS NULL OR moved_out > moved_in);

ALTER TABLE personnel
    ADD CONSTRAINT chk_clearance_level CHECK (clearance_level BETWEEN 0 AND 5);

ALTER TABLE incidents
    ADD CONSTRAINT chk_severity CHECK (severity BETWEEN 1 AND 5);

ALTER TABLE users
    ADD CONSTRAINT chk_role CHECK ("ROLE" IN ('O5', 'RESEARCHER'));

-- Foreign keys (Firebird не поддерживает множественный ADD CONSTRAINT в одном ALTER)
ALTER TABLE containment_history ADD CONSTRAINT fk_history_scp FOREIGN KEY (scp_id) REFERENCES scp_objects(id);
ALTER TABLE containment_history ADD CONSTRAINT fk_history_site FOREIGN KEY (site_id) REFERENCES containment_sites(id);

ALTER TABLE personnel ADD CONSTRAINT fk_personnel_site FOREIGN KEY (base_site_id) REFERENCES containment_sites(id);

ALTER TABLE mtf_teams ADD CONSTRAINT fk_mtf_site FOREIGN KEY (base_site_id) REFERENCES containment_sites(id);

ALTER TABLE mtf_members ADD CONSTRAINT fk_member_mtf FOREIGN KEY (mtf_id) REFERENCES mtf_teams(id) ON DELETE CASCADE;
ALTER TABLE mtf_members ADD CONSTRAINT fk_member_personnel FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE;

ALTER TABLE incidents ADD CONSTRAINT fk_incident_scp FOREIGN KEY (scp_id) REFERENCES scp_objects(id);
ALTER TABLE incidents ADD CONSTRAINT fk_incident_site FOREIGN KEY (site_id) REFERENCES containment_sites(id);
ALTER TABLE incidents ADD CONSTRAINT fk_incident_mtf FOREIGN KEY (mtf_id) REFERENCES mtf_teams(id) ON DELETE SET NULL;

ALTER TABLE procedure_revisions ADD CONSTRAINT fk_proc_scp FOREIGN KEY (scp_id) REFERENCES scp_objects(id);
ALTER TABLE procedure_revisions ADD CONSTRAINT fk_proc_approver FOREIGN KEY (approved_by_id) REFERENCES personnel(id);

ALTER TABLE users ADD CONSTRAINT fk_user_personnel FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE;
