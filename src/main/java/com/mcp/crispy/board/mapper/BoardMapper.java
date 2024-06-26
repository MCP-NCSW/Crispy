package com.mcp.crispy.board.mapper;

import com.mcp.crispy.board.dto.BoardDto;
import com.mcp.crispy.board.dto.BoardFileDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;


@Mapper
public interface BoardMapper {

    int deleteLike(int boardNo, int empNo);
    int insertLike(int boardNo, int empNo);
    int likeCheck(int boardNo);
    // 자유게시판 LIST
    List<BoardDto> getFreeBoardList(Map<String, Object> map);

    int getTotalCount(String search);

    int insertBoard(BoardDto board);

    int insertBoardFile(BoardFileDto BoardFile);

    int getBoardCount();

    List<BoardDto> getBoardList(Map<String, Object> map);

    BoardDto getBoardByNo(int BoardNo);

    List<BoardFileDto> getBoardFileList(int BoardNo);

    BoardFileDto getBoardFileByNo(int BoardFileNo);

    //    int updateDownloadCount(int BoardFileNo);
    int updateBoard(BoardDto Board);

    int deleteBoardFile(int BoardFileNo);

    int deleteBoard(int BoardNo);

    int updateHit(int boardNo);


    int checkLikeStatus(int boardNo, int empNo);

    int getLikeCount(int boardNo);

    void deleteLikesByBoardNo(int boardNo);
}