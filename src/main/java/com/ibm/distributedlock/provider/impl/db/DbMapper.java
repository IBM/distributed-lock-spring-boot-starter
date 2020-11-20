package com.ibm.distributedlock.provider.impl.db;


import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author seanyu
 */
@Mapper
public interface DbMapper {
    /**
     * insert lock key-value
     * @param lockKey
     * @param lockValue
     * @return
     */
    @Insert("INSERT INTO distributed_lock (lock_key, lock_value) VALUES ( #{lockKey}, #{lockValue}) ")
    int insert(@Param("lockKey") String lockKey, @Param("lockValue") String lockValue);

    /**
     * delete lock key-value
     * @param lockKey
     * @param lockValue
     * @return
     */
    @Delete("DELETE FROM distributed_lock WHERE lock_key = #{lockKey} AND lock_value = #{lockValue} ")
    int deleteByKeyAndValue(@Param("lockKey") String lockKey, @Param("lockValue") String lockValue);
}
