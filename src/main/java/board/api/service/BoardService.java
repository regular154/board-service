package board.api.service;

import board.api.dao.BoardDao;
import board.api.elastic.BoardFilter;
import board.api.elastic.BoardPageParameters;
import board.api.elastic.BoardResponse;
import board.api.model.Board;
import org.elasticsearch.action.update.UpdateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Configuration
public class BoardService {

    private final BoardDao boardDao;

    @Autowired
    public BoardService(BoardDao boardDao) {
        this.boardDao = boardDao;
    }

    public List<BoardFilter> getFilters () {
        return boardDao.findFilters();
    }

    public Board getBoardById (String id) {
        return boardDao.findBoardById(id);
    }

    public boolean removeBoardById (String id) {
        return boardDao.removeBoard(id);
    }

    public String addBoard (Board board) {
        return boardDao.createBoard(board);
    }

    public UpdateResponse updateBoard (String id, HashMap<String, Object> body) {
        return boardDao.updateBoard(id, body);
    }

    public boolean addRandomBoards(int num) {
        return boardDao.addRandomBoards(num);
    }

    public boolean deleteAllBoards() {
        return boardDao.deleteAllBoards();
    }

    public BoardResponse getBoardsByFilters(BoardPageParameters pageParameters, Map<String, String> searchFilters) {
        return boardDao.findBoards(pageParameters, searchFilters);
    }

    public BoardResponse searchBoars(BoardPageParameters pageParameters, String q) {
        return boardDao.searchBoards(pageParameters, q);
    }
}
