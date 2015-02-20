(SELECT enName as value, id FROM City WHERE countryId = :countryId AND enName like :query)
UNION
(SELECT ruName as value, id FROM City WHERE countryId = :countryId AND ruName like :query)
ORDER BY id ASC LIMIT :limit