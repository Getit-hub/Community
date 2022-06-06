package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);

    int insertDiscussPost(DiscussPost post);

    DiscussPost selectDiscussPostById(int id);

    int updateCommentCount(int id, int commentCount);

    /**
     * 根据id修改帖子类型 比如帖子设为加精 或者拉黑
     * @param id
     * @param type
     * @return
     */
    @Update("update discuss_post set type = #{type} where id = #{id}")
    int updateType(int id, int type);

    /**
     * 根据id修改帖子状态 比如置顶
     * @param id
     * @param status
     * @return
     */
    @Update("update discuss_post set status = #{status} where id = #{id}")
    int updateStatus(int id, int status);
}
