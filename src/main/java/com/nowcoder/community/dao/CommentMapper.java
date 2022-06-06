package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    int selectCountByEntity(int entityType, int entityId);

    int insertComment(Comment comment);

    Comment selectCommentById(int id);


    /**
     * 查询某个用户的评论（包括回复）
     */

    @Select("select * from comment where user_id = #{userId} order by create_time desc limit #{offset},#{limit}")
    List<Comment> selectCommentsByUserId(int userId,int offset,int limit);


    /**
     * 查询某个用户的评论数量
     * @param userId
     * @return
     */
    @Select("select count(*) from comment where user_id = #{userId}")
    int selectCommentCountByUserId(int userId);






}
