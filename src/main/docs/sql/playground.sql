SELECT
	d.path,
	d.id,
	f.filename,
	f.id,
	f.size,
	f.scandate,
	f.sha1,
	f.lastmodified
FROM files f
INNER JOIN directories d
	ON d.id = f.dir_id
WHERE EXISTS (
	SELECT ff1.id
	FROM files ff1
	INNER JOIN files ff2
		ON ff2.sha1 = ff1.sha1 
		AND ff2.size = ff1.size
		AND ff1.id <> ff2.id
	INNER JOIN directories dd1
		ON dd1.id = ff1.dir_id
	INNER JOIN directories dd2
		ON dd2.id = ff2.dir_id
	WHERE ff1.sha1 = f.sha1
	AND dd1.path LIKE '/media/kaefert/0e266b8e-2c8c-4833-8231-ecc4c473c257%'
	AND dd2.path LIKE '/media/kaefert/f18a06aa-5974-458a-aa2b-9d8024c31058%'
	AND ff1.lastmodified > ff2.lastmodified
)
ORDER BY sha1

/*  */

