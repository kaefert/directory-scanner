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
AND f.size > 0
AND EXISTS ( /* other files with the same sha1 exist (duplicates) */ 
	SELECT ff.id 
	FROM files ff 
	WHERE ff.sha1 = f.sha1
	AND ff.size = f.size
	AND ff.id <> f.id 
	AND (ff.dir_id <> f.dir_ID OR ff.filename <> f.filename) /*other dir or other filename */
)