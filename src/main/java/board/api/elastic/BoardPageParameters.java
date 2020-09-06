package board.api.elastic;

import lombok.Data;

@Data
public class BoardPageParameters {
    private int page;
    private int pageSize;
    private String sortBy;
    private String sortOrder;
}
