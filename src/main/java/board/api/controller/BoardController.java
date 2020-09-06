package board.api.controller;

import board.api.elastic.BoardPageParameters;
import board.api.elastic.BoardResponse;
import board.api.model.Board;
import board.api.service.BoardService;
import org.elasticsearch.action.update.UpdateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/boards")
@PropertySource("classpath:board.properties")
public class BoardController {

    private final BoardService boardService;

    @Value("${board.search.filters}")
    private List<String> filterNames;
    @Value("${board.page.request.parameters}")
    private List<String> pageParameterNames;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @PostMapping(value = "/addRandom/{num}")
    public ResponseEntity<HashMap<String, Object>> addBoard(@PathVariable(value = "num") int num) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("result", boardService.addRandomBoards(num));
        return ResponseEntity.ok(result);
    }

    /**
     * Get board by id
     *
     * @param boardId - board id
     * @return board
     */
    @GetMapping(value = "/{boardId}")
    public ResponseEntity<HashMap<String, Object>> getBoardById(@PathVariable(value = "boardId") String boardId) {
        HashMap<String, Object> result = new HashMap<>();
        Board board = boardService.getBoardById(boardId);
        result.put("value", board);
        return ResponseEntity.ok(result);
    }

    /**
     * Remove order
     *
     * @param boardId - board id
     * @return result - deleted or not deleted
     */
    @DeleteMapping(value = "/{boardId}")
    public ResponseEntity<HashMap<String, Object>> removeBoardById(@PathVariable(value = "boardId") String boardId) {
        HashMap<String, Object> result = new HashMap<>();
        if (boardService.removeBoardById(boardId)) {
            result.put("result", true);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Add new board to database
     *
     * @param board - board
     * @return result - added or not added
     */
    @PostMapping
    public ResponseEntity<HashMap<String, Object>> addBoard(@RequestBody Board board) {
        HashMap<String, Object> result = new HashMap<>();
        String id = boardService.addBoard(board);
        result.put("id", id);
        return ResponseEntity.ok(result);
    }

    @PutMapping(value = "/{boardId}")
    public ResponseEntity<HashMap<String, Object>> updateBoard(
            @PathVariable(value = "boardId") String boardId,
            @RequestBody HashMap<String, Object> body) {
        HashMap<String, Object> result = new HashMap<>();
        UpdateResponse updateResponse = boardService.updateBoard(boardId, body);
        result.put("result", updateResponse.getGetResult());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<HashMap<String, Object>> findBoards(@RequestParam Map<String, String> requestParams) {
        HashMap<String, Object> results = new HashMap<>();
        validateRequestParams(requestParams);
        BoardPageParameters pageParameters = getBoardPageParameters(requestParams);
        Map<String, String> searchFilters = getSearchFilters(requestParams);
        BoardResponse response = boardService.getBoardsByFilters(pageParameters, searchFilters);
        results.put("result", response);
        return ResponseEntity.ok(results);
    }

    /**
     * Get search filters
     *
     * @return search filters
     */
    @GetMapping(value = "/filters")
    public ResponseEntity<HashMap<String, Object>> getFilters() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("filters", boardService.getFilters());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    public ResponseEntity<HashMap<String, Object>> deleteAllBoards() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("result", boardService.deleteAllBoards());
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/search")
    public ResponseEntity<HashMap<String, Object>> searchBoards(@RequestParam Map<String, String> requestParams) {
        HashMap<String, Object> results = new HashMap<>();
        validateRequestParams(requestParams);
        results.put("result", boardService.searchBoars(getBoardPageParameters(requestParams), requestParams.get("q")));
        return ResponseEntity.ok(results);
    }

    private void validateRequestParams(Map<String, String> requestParams) {
        requestParams.keySet().forEach(this::validateParameter);
    }

    private void validateParameter(String param) {
        if (!pageParameterNames.contains(param)
                && !filterNames.contains(param)
                && !"q".equals(param)) {
            throw new BadRequestParameterException("Bad request parameter: " + param);
        }
    }

    private Map<String, String> getSearchFilters(Map<String, String> requestParams) {
        Map<String, String> searchFilters = new HashMap<>();
        requestParams.forEach((key, value) -> {
            if (filterNames.contains(key)) {
                searchFilters.put(key, value);
            }
        });
        return searchFilters;
    }

    private BoardPageParameters getBoardPageParameters(Map<String, String> requestParams) {
        BoardPageParameters parameters = new BoardPageParameters();
        parameters.setPage(Optional.ofNullable(requestParams.get("page")).map(Integer::parseInt).orElse(0));
        parameters.setPageSize(Optional.ofNullable(requestParams.get("pageSize")).map(Integer::parseInt).orElse(10));
        parameters.setSortBy(Optional.ofNullable(requestParams.get("sortBy")).orElse("year"));
        parameters.setSortOrder(Optional.ofNullable(requestParams.get("sortOrder")).orElse("desc"));
        return parameters;
    }

}
