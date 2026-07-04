package com.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortlink.entity.ShortLink;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLink> {
    @Select("SELECT * FROM short_link WHERE original_url = #{originalUrl} ORDER BY create_time DESC LIMIT 1")
    ShortLink findLatestByOriginalUrl(@Param("originalUrl") String originalUrl);

    @Select("SELECT * FROM short_link WHERE short_code = #{shortCode} LIMIT 1")
    ShortLink findByShortCode(@Param("shortCode") String shortCode);

    @Select("SELECT short_code FROM short_link WHERE short_code IS NOT NULL")
    List<String> findAllShortCodes();

    @Select("SELECT * FROM short_link WHERE short_code IS NOT NULL ORDER BY create_time DESC LIMIT #{limit}")
    List<ShortLink> findRecent(@Param("limit") int limit);

    @Update("UPDATE short_link SET visit_count = visit_count + #{delta} WHERE short_code = #{shortCode}")
    int incrementVisitCount(@Param("shortCode") String shortCode, @Param("delta") long delta);

    @Delete("DELETE FROM short_link WHERE expire_time IS NOT NULL AND expire_time < DATE_SUB(NOW(), INTERVAL 7 DAY)")
    int deleteExpiredBeforeRetention();
}
