package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.RagChunk;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RagChunkMapper {

    long countAll();

    long countWithoutEmbedding();

    List<RagChunk> selectAll();

    int deleteByPostId(@Param("postId") Long postId);

    int deleteAll();

    int batchInsert(@Param("chunks") List<RagChunk> chunks);
}
