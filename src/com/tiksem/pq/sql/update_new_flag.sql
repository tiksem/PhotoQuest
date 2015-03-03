UPDATE photoquest SET isNew = FALSE WHERE isNew = TRUE AND addingDate < :startTime;
UPDATE photo SET isNew = FALSE WHERE isNew = TRUE AND addingDate < :startTime;