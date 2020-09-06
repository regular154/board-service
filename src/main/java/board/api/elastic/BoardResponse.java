package board.api.elastic;

import board.api.model.Board;
import lombok.Data;

import java.util.List;

@Data
public class BoardResponse {
    private List<Board> values;
    private List<BoardFilter> filters;
    private int page;
    private int pageSize;
    private long total;
    private int totalPages;
}
