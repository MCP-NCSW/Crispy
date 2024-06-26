package com.mcp.crispy.board.service;

import com.mcp.crispy.board.dto.BoardDto;
import com.mcp.crispy.board.dto.BoardFileDto;
import com.mcp.crispy.board.mapper.BoardMapper;
import com.mcp.crispy.common.utils.MyFileUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Transactional
@RequiredArgsConstructor
@Service
public class BoardService {

    private final BoardMapper boardMapper;
    private final MyFileUtils myFileUtils;

    @Transactional
    public int registerBoard(String title, String content, int empNo, int boardCtNo, int creator) {
        // Create BoardDto instance
        BoardDto board = BoardDto.builder()
                .boardCtNo(boardCtNo) // Assuming 자유게시판 카테고리가 1로 정의되어 있음
                .boardTitle(title)
                .boardContent(content)
                .empNo(empNo)
                .creator(empNo)
                .build();

        // Insert board and retrieve generated boardNo
        int insertBoardCount = boardMapper.insertBoard(board);

        // Check if board was successfully inserted
        if (insertBoardCount == 1) {
            // Retrieve and return generated boardNo
            return board.getBoardNo();
        }

        // Return -1 if board insertion fails
        return -1;
    }

    // BoardService 클래스에 registerBoardFile 메서드 수정
    @Transactional
    public boolean registerBoardFile(MultipartHttpServletRequest multipartRequest, int boardNo) {
        // Retrieve boardFileed files
        List<MultipartFile> files = multipartRequest.getFiles("files");

        // Count of successfully inserted board files
        int insertBoardFileCount = 0;

        // Iterate over boardFileed files and process them
        for (MultipartFile multipartFile : files) {
            if (multipartFile != null && !multipartFile.isEmpty()) {
                try {
                    // Save the file to disk
                    String boardPath = myFileUtils.getBoardPath();
                    File dir = new File(boardPath);
                    if (!dir.exists()) {
                        dir.mkdirs();  // Create directories if they do not exist
                    }

                    String boardOrigin = multipartFile.getOriginalFilename();
                    String boardRename = myFileUtils.getBoardRename(boardOrigin);
                    File file = new File(boardPath, boardRename);
                    multipartFile.transferTo(file);

                    // Create BoardFileDto instance
                    BoardFileDto boardFile = BoardFileDto.builder()
                            .boardRename(boardRename)
                            .boardOrigin(boardOrigin)
                            .boardPath(boardPath)
                            .boardNo(boardNo) // Use the generated boardNo
                            .build();

                    // Insert board file
                    int inserted = boardMapper.insertBoardFile(boardFile);
                    if (inserted == 1) {
                        insertBoardFileCount++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Ensure all files were successfully inserted
        return insertBoardFileCount == files.size();
    }

    @Transactional(readOnly = true)
    public List<BoardDto> getFreeBoardList(Integer page, int cnt, String search) {
        int totalCount = getTotalCount(search);
        int total = totalCount / cnt + ((totalCount % cnt > 0) ? 1 : 0);
        int begin = (page - 1) * cnt + 1;
        int end = begin + cnt - 1;
        if (search == null) {
            search = ""; // Set default value if null
        }
        Map<String, Object> map = Map.of("begin", begin, "end", end, "total", total, "search", search);

        return boardMapper.getFreeBoardList(map);
    }


    @Transactional(readOnly = true)
    public int getTotalCount(String search) {
        return boardMapper.getTotalCount(search);
    }


    public ResponseEntity<Resource> download(int boardFileNo, HttpServletRequest request) {
        BoardFileDto boardFile = boardMapper.getBoardFileByNo(boardFileNo);
        File file = new File(boardFile.getBoardPath(), boardFile.getBoardRename());
        Resource resource = new FileSystemResource(file);

        if (!resource.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String boardOrigin = boardFile.getBoardOrigin();
        String userAgent = request.getHeader("User-Agent");
        try {
            if (userAgent.contains("Trident")) {
                boardOrigin = URLEncoder.encode(boardOrigin, "UTF-8").replace("+", " ");
            } else if (userAgent.contains("Edg")) {
                boardOrigin = URLEncoder.encode(boardOrigin, "UTF-8");
            } else {
                boardOrigin = new String(boardOrigin.getBytes("UTF-8"), "ISO-8859-1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.add("Content-Disposition", "boardFilement; filename=" + boardOrigin);
        responseHeader.add("Content-Length", String.valueOf(file.length()));

        return new ResponseEntity<>(resource, responseHeader, HttpStatus.OK);
    }

    public ResponseEntity<Resource> downloadAll(int boardNo, HttpServletRequest request) {
        List<BoardFileDto> boardFileList = boardMapper.getBoardFileList(boardNo);
        if (boardFileList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        String tempFilename = "boardFiles_" + boardNo + ".zip";
        File tempFile = new File(tempDir, tempFilename);

        try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(tempFile))) {
            for (BoardFileDto boardFile : boardFileList) {
                ZipEntry zipEntry = new ZipEntry(boardFile.getBoardOrigin());
                zout.putNextEntry(zipEntry);

                try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(boardFile.getBoardPath(), boardFile.getBoardRename())))) {
                    zout.write(in.readAllBytes());
                }

                zout.closeEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Resource resource = new FileSystemResource(tempFile);
        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.add("Content-Disposition", "boardFilement; filename=" + tempFilename);
        responseHeader.add("Content-Length", String.valueOf(tempFile.length()));

        return new ResponseEntity<>(resource, responseHeader, HttpStatus.OK);
    }


    @Transactional(readOnly = true)

    public BoardDto getBoardByNo(int boardNo) {

        updateHit(boardNo);
        return boardMapper.getBoardByNo(boardNo);
    }

    @Transactional(readOnly = true)
    public void loadBoardByNo(int boardNo, Model model) {
        model.addAttribute("board", boardMapper.getBoardByNo(boardNo));
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@" + boardMapper.getBoardByNo(boardNo));
        model.addAttribute("boardFileList", boardMapper.getBoardFileList(boardNo));
    }


    public int modifyBoard(BoardDto board) {
        return boardMapper.updateBoard(board);
    }

    @Transactional(readOnly = true)

    public ResponseEntity<Map<String, Object>> getBoardFileList(int boardNo) {
        return ResponseEntity.ok(Map.of("boardFileList", boardMapper.getBoardFileList(boardNo)));
    }

    public ResponseEntity<Map<String, Object>> addBoardFile(MultipartHttpServletRequest multipartRequest) throws Exception {

        List<MultipartFile> files = multipartRequest.getFiles("files");

        int boardFileCount;
        if (files.get(0).getSize() == 0) {
            boardFileCount = 1;
        } else {
            boardFileCount = 0;
        }

        for (MultipartFile multipartFile : files) {

            if (multipartFile != null && !multipartFile.isEmpty()) {

                String boardPath = myFileUtils.getBoardPath();
                File dir = new File(boardPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String boardOrigin = multipartFile.getOriginalFilename();
                String boardRename = myFileUtils.getBoardRename(boardOrigin);
                File file = new File(dir, boardRename);

                multipartFile.transferTo(file);

                String contentType = Files.probeContentType(file.toPath());  // 이미지의 Content-Type은 image/jpeg, image/png 등 image로 시작한다.

                BoardFileDto boardFile = BoardFileDto.builder()
                        .boardOrigin(boardOrigin)
                        .boardRename(boardRename)
                        .boardNo(Integer.parseInt(multipartRequest.getParameter("boardNo")))
                        .boardPath(boardPath)
                        .build();

                boardFileCount += boardMapper.insertBoardFile(boardFile);

            }  // if

        }  // for

        return ResponseEntity.ok(Map.of("boardFileResult", files.size() == boardFileCount));

    }
//
//    public ResponseEntity<Map<String, Object>> removeBoardFile(int boardFileNo) {
//        // 삭제할 첨부 파일 정보를 DB에서 가져오기
//        BoardFileDto boardFile = boardMapper.getBoardFileByNo(boardFileNo);
//
//        // 파일 삭제
//        File file = new File(boardFile.getBoardPath(), boardFile.getBoardRename());
//        if (file.exists()) {
//            file.delete();
//        }
//
//        // DB에서 첨부 파일 삭제
//        int deleteCount = boardMapper.deleteBoardFile(boardFileNo);
//
//        return ResponseEntity.ok(Map.of("deleteCount", deleteCount));
//    }

    public ResponseEntity<Map<String, Object>> removeBoardFile(int boardFileNo) {
        // Retrieve boardFile object from the database using boardFileNo
        BoardFileDto boardFile = boardMapper.getBoardFileByNo(boardFileNo);

        if (boardFile == null) {
            // Handle the case where boardFile is null, possibly log an error or return appropriate response
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Board file not found"));
        }

        // Now proceed with deleting the file and updating the database
        if (boardFile.getBoardPath() != null && boardFile.getBoardRename() != null) {
            String boardPath = boardFile.getBoardPath();
            String boardRename = boardFile.getBoardRename();

            // Delete the physical file from the file system
            File file = new File(boardPath, boardRename);
            if (file.exists()) {
                file.delete();
            }

            // Delete the board file record from the database
            int deleteCount = boardMapper.deleteBoardFile(boardFileNo);

            // Return response indicating successful deletion
            return ResponseEntity.ok(Map.of("deleteCount", deleteCount));
        } else {
            // Handle the case where boardPath or boardRename is null
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Board file path or name is null"));
        }
    }

    public int removeBoard(int boardNo) {
        // Get the list of files associated with the board
        List<BoardFileDto> boardFileList = boardMapper.getBoardFileList(boardNo);

        // Delete each file from the filesystem
        if (boardFileList != null && !boardFileList.isEmpty()) {
            for (BoardFileDto boardFile : boardFileList) {
                File file = new File(boardFile.getBoardPath(), boardFile.getBoardRename());
                if (file.exists()) {
                    file.delete();
                }
            }
        }

        // Delete each file record from the database
        if (boardFileList != null && !boardFileList.isEmpty()) {
            for (BoardFileDto boardFile : boardFileList) {
                boardMapper.deleteBoardFile(boardFile.getBoardFileNo());
            }
        }
        removeLikesByBoardNo(boardNo);

        // Delete the board from the database
        return boardMapper.deleteBoard(boardNo);
    }
    public void removeLikesByBoardNo(int boardNo) {
        boardMapper.deleteLikesByBoardNo(boardNo);
    }
    @Transactional
    public int updateHit(int boardNo) {
        return boardMapper.updateHit(boardNo);
    }

    public int checkLikeStatus(int boardNo, int empNo) {
        return boardMapper.checkLikeStatus(boardNo, empNo);
    }

    public int getLikeCount(int boardNo) {
        return boardMapper.getLikeCount(boardNo);
    }

    public int like(boolean check, int boardNo, int empNo) {
        if (check) {
            boardMapper.insertLike(boardNo, empNo);
        } else {
            boardMapper.deleteLike(boardNo, empNo);
        }
        return boardMapper.getLikeCount(boardNo);
    }


}