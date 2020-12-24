# distributed-lock-spring-boot-starter

This is a distributed reentrant lock spring boot starter(only test for spring boot 2.+).

This distributed reentrant lock currently provides two implementions--db and redis(singleton). Redis cluster version will be implement in future release.

## feature

- distributed
- reentrant
- provides expire time(only in redis version)

## Add dependency using Gradle:

```gradle
dependencies {
  compile('com.ibm.esw:distributed-lock-spring-boot-starter:0.0.1-SNAPSHOT')
}
```

## How to use:

### Config

First you need to config DistributedReentrantLock in application.yml:

```yml
# for redis version
spring:
  redis:
    host: localhost
    port: 6379
distributed-lock:
  enabled: true
  lock-type: redis
  
# for db version 
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/distributed_lock
    username: root
    password: root
    driver-class-name: com.mysql.jdbc.Driver
distributed-lock:
  enabled: true
  lock-type: redis
```

if you are using redis version, config application.yml is all you need to do.

if you are using db version, you also need to create a table:

```sql
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
```

### Code

```java
    @Autowired
    private Function<String, DistributedReentrantLock> distributedReentrantLockFunction;
    
    Lock lock = distributedReentrantLockFunction.apply("yourLockKey");
    
    lock.lock();
    lock.lock(); // support reentrant
    
    // do something
    
    lock.unlock();
    lock.unlock();
    
    
    
    // with expire time
    lock.lockWithExpireTime(5, TimeUnit.SECONDS);
    lock.lockWithExpireTime(5, TimeUnit.SECONDS); // support reentrant
    
    // do something
    
    lock.unlock();
    lock.unlock();
```

Please enjoy.
