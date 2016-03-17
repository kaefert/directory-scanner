CREATE TABLE IF NOT EXISTS directories ( 
     id INT NOT NULL AUTO_INCREMENT PRIMARY KEY 
    ,path VARCHAR(1000) NOT NULL 
	,scanDir_id INT NULL 
	,scandate DateTime NOT NULL 
	,finished DateTime NULL 
	,FOREIGN KEY (scanDir_id) REFERENCES directories(id) ON DELETE CASCADE 
) ENGINE=InnoDB;