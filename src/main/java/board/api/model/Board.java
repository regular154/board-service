package board.api.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;

@Data
@Document(indexName = "snowboard", type = "_doc")
public class Board {
    @Id
    private String id;
    private int year;
    private String name;
    private String code;
    private String size;
    private Spec spec;
    private List<Feature> features;
    private String terrain;
    private String ridingLevel;
    private String bend;
    private String shape;
    private String details;
}
