-- Architectural Characteristics
INSERT INTO architecturalcharacteristic(id, name, description) values (1, 'DATA_CONSISTENCY', 'The quality of relations of data values'); -- Defined in ISO/IEC 25012 - Data Quality model.

-- Architectural Patterns
-- Data Consistency
INSERT INTO architecturalpattern(id, name, description, architectural_characteristic_id) VALUES (1, 'SAGA', 'Sequence of local transactions that are coordinated using messaging.', 1); -- Defined in Richardson, C. (2019). Microservices Patterns. MANNING, page 58

-- Architectural Tactics
-- Data Consistency
INSERT INTO architecturaltactic(id, name, description, architectural_pattern_id, architectural_characteristic_id) VALUES(1, 'SAGA_PATTERN_COMPENSATING_TRANSACTIONS', 'If the n plus one saga transaction failed, then the n previous transactions must be undone (rollback changes).', 1, 1); -- Defined in Richardson, C. (2019). Microservices Patterns. MANNING, page 116