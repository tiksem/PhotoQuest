(SELECT * FROM City WHERE countryId = :countryId AND enName like :query)
UNION
(SELECT * FROM City WHERE countryId = :countryId AND ruName like :query)
ORDER BY id desc LIMIT :limit