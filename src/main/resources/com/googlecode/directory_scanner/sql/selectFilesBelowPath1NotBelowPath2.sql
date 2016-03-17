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
WHERE d.path LIKE ? 
AND NOT EXISTS( 
	SELECT ff.id 
	FROM files ff 
	INNER JOIN directories dd 
		ON dd.id = ff.dir_id 
	WHERE ff.sha1 = f.sha1 
	AND dd.path LIKE ? 
)