SELECT 
	d.path, 
	f.filename, 
	f.size, 
	f.scandate, 
	f.lastmodified 
FROM files f 
INNER JOIN directories d 
	ON d.id = f.dir_id 
WHERE f.sha1 = ?