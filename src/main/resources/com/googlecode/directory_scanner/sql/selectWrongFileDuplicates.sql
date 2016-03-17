SELECT 
  f.id, f.dir_id, f.filename 
FROM files AS f 
INNER JOIN files AS ff 
   ON f.id <> ff.id 
   AND f.dir_id=ff.dir_id 
   AND f.filename=ff.filename
