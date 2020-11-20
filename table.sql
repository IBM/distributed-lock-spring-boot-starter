--mysql--
create table distributed_lock
(
	id int auto_increment
		primary key,
	lock_key varchar(255) not null,
	lock_value varchar(255) not null,
	constraint uidx_order_id
		unique (lock_key)
);

--db2--
CREATE TABLE distributed_lock
(
  id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY(START WITH 1 INCREMENT BY 1),
  lock_key VARCHAR(255) NOT NULL,
  lock_value VARCHAR(225) NOT NULL
);
CREATE UNIQUE INDEX distributed_lock_lock_key_uindex ON distributed_lock (lock_key);