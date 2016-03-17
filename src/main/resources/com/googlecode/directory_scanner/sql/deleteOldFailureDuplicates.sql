DELETE FROM failures f 
WHERE EXISTS ( 
  SELECT f2.id 
  FROM failures f2 
  WHERE f2.path = f.path 
  AND f2.scandate > f.scandate 
)