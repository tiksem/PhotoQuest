SELECT ::nameField as value, id FROM City WHERE countryId = :countryId
ORDER BY id asc
LIMIT :limit;