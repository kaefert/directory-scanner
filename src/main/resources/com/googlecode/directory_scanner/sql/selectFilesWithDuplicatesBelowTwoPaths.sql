/**** ASSUMPTIONS ****/
/* if one of the paths is contained in the other, the more general one must be the first! */
/* the second path needs to be given as parameter 2 & 3 */

SELECT
/* selected file has no path limit */
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
WHERE EXISTS ( /* 2 distinct duplicates below 2 paths given by parameter */
	SELECT f1.id
	FROM files f1
	INNER JOIN files f2
		ON f2.sha1 = f1.sha1 
		AND f2.size = f1.size
		AND f1.id <> f2.id
	INNER JOIN directories d1
		ON d1.id = f1.dir_id
	INNER JOIN directories d2
		ON d2.id = f2.dir_id
	WHERE f1.sha1 = f.sha1
	  AND f1.size = f.size
	  AND d1.path LIKE ?
	  AND d2.path LIKE ?
/* subselect is not closed here, added filter code closes it */
