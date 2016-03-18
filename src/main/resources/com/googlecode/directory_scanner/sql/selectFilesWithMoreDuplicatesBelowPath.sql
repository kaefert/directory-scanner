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
WHERE ( /* at least given number of those duplicates are within the given path */ 
	SELECT count(ff2.id) 
	FROM files ff2 
	INNER JOIN directories dd2 
		ON dd2.id = ff2.dir_id 
	WHERE ff2.sha1 = f.sha1 
	AND dd2.path LIKE ? 
) >= ?
AND f.size > 0