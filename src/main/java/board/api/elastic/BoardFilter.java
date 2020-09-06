package board.api.elastic;

import lombok.Data;

import java.util.Map;

@Data
public class BoardFilter {
    private String name;
    private Map<String, Long> values;
    private String selectedValue;

    public BoardFilter(String name, Map<String, Long> values, String selectedValue) {
        this.name = name;
        this.values = values;
        this.selectedValue = selectedValue;
    }

    public BoardFilter(String name, Map<String, Long> values) {
        this.name = name;
        this.values = values;
    }
}
