
/**************************************************/
/************ sql_selectFilesBelowPath ************/
/**************************************************/
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

/**************************************************/
/***** sql_selectFilesBelowPath1NotBelowPath2 *****/
/**************************************************/
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

/**************************************************/
/***** sql_selectFilesWithDuplicatesBelowPath *****/
/**************************************************/
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
WHERE EXISTS ( /* other files with the same sha1 exist (duplicates) */ 
	SELECT ff.id 
	FROM files ff 
	INNER JOIN directories dd 
		ON dd.id = ff.dir_id 
	WHERE ff.sha1 = f.sha1 
	AND ff.id <> f.id 
) AND EXISTS ( /* one of those duplicates is within the given path */ 
	SELECT ff2.id 
	FROM files ff2 
	INNER JOIN directories dd2 
		ON dd2.id = ff2.dir_id 
	WHERE ff2.sha1 = f.sha1 
	AND dd2.path LIKE ? 
)

/**************************************************/
/*** sql_selectFilesWithMoreDuplicatesBelowPath ***/
/**************************************************/
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

/**************************************************/
/*** sql_selectFilesWithDuplicatesBelowTwoPaths ***/
/**************************************************/
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
WHERE EXISTS ( /* other files with the same sha1 exist (duplicates) */ 
	SELECT ff.id 
	FROM files ff 
	INNER JOIN directories dd 
		ON dd.id = ff.dir_id 
	WHERE ff.sha1 = f.sha1 
	AND ff.id <> f.id 
) AND EXISTS ( /* one of those duplicates is within the given path1 */ 
	SELECT ff2.id 
	FROM files ff2 
	INNER JOIN directories dd2 
		ON dd2.id = ff2.dir_id 
	WHERE ff2.sha1 = f.sha1 
	AND dd2.path LIKE ? 
) AND EXISTS ( /* one of those duplicates is within the given path2 */ 
	SELECT ff3.id 
	FROM files ff3 
	INNER JOIN directories dd3 
		ON dd3.id = ff3.dir_id 
	WHERE ff3.sha1 = f.sha1 
	AND dd3.path LIKE ? 
)

/**************************************************/
/************ sql_selectSha1Collisions ************/
/**************************************************/
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
WHERE EXISTS ( /* same sha1 but different size */ 
	SELECT ff.id 
	FROM files ff 
	INNER JOIN directories dd 
		ON dd.id = ff.dir_id 
	WHERE ff.sha1 = f.sha1 
	AND ff.size <> f.size 
) 
OR EXISTS ( /* files with same name and path but different id */ 
	SELECT ff2.id 
	FROM files ff2 
	INNER JOIN directories dd2 
		ON dd2.id = ff2.dir_id 
	WHERE ff2.id <> f.id 
	AND ff2.filename = f.filename 
	AND dd2.path = d.path 
) 
ORDER BY f.sha1
