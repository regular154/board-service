package board.api.dao;

import board.api.elastic.BoardFilter;
import board.api.elastic.BoardPageParameters;
import board.api.elastic.BoardResponse;
import board.api.model.Board;
import board.api.util.BoardUtil;
import io.netty.util.internal.StringUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Component
@PropertySource("classpath:board.properties")
public class BoardDao {

    private final ElasticsearchOperations elasticsearchOperations;
    private final BoardUtil boardUtil;
    private static final int FILTER_SIZE = 100;
    @Value("${board.search.filters}")
    private List<String> filterNames;
    @Value("${board.search.fields}")
    private List<String> searchFields;

    public BoardDao(ElasticsearchOperations elasticsearchOperations, BoardUtil boardUtil) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.boardUtil = boardUtil;
    }

    public String createBoard(Board board) {
        board.setId(UUID.randomUUID().toString());
        return elasticsearchOperations.index(new IndexQueryBuilder()
                .withId(board.getId())
                .withObject(board)
                .build());
    }

    public Board findBoardById(String id) {
        return elasticsearchOperations.queryForObject(GetQuery.getById(id), Board.class);
    }

    public UpdateResponse updateBoard(String id, HashMap<String, Object> body) {
        UpdateQuery updateQuery = new UpdateQueryBuilder()
                .withId(id)
                .withClass(Board.class)
                .withUpdateRequest(new UpdateRequest().doc(body))
                .build();
        return elasticsearchOperations.update(updateQuery);
    }

    public boolean removeBoard(String id) {
        return id.equals(elasticsearchOperations.delete(Board.class, id));
    }

    public Page<Board> findBoardsByYear(int year, int page, int pageSize, String sortBy, String order) {
        FieldSortBuilder sortBuilder = SortBuilders
                .fieldSort(sortBy)
                .order(SortOrder.fromString(order));
        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("year", year))
                .withPageable(PageRequest.of(page, pageSize))
                .withSort(sortBuilder)
                .build();
        return elasticsearchOperations.queryForPage(query, Board.class);
    }

    public boolean deleteAllBoards() {
        DeleteQuery deleteQuery = new DeleteQuery();
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        deleteQuery.setQuery(queryBuilder);
        elasticsearchOperations.delete(deleteQuery, Board.class);
        return true;
    }

    public List<BoardFilter> findFilters() {
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(matchAllQuery());
        addFilterAggregations(nativeSearchQueryBuilder);
        SearchQuery searchQuery = nativeSearchQueryBuilder.build();
        Aggregations aggregations = elasticsearchOperations.query(searchQuery, SearchResponse::getAggregations);
        return getFilters(aggregations, Collections.emptyMap());
    }

    public boolean addRandomBoards(int num) {
        elasticsearchOperations.bulkIndex(
                Stream.generate(this::getBoardIndexQuery)
                        .limit(num)
                        .collect(Collectors.toList()));
        return true;
    }

    private IndexQuery getBoardIndexQuery() {
        Board board = boardUtil.getRandomBoard();
        return new IndexQueryBuilder()
                .withId(board.getId())
                .withObject(board)
                .build();
    }

    public BoardResponse findBoards(BoardPageParameters pageParameters, Map<String, String> searchFilters) {
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(getQueryForFilters(searchFilters))
                .withPageable(getPageable(pageParameters))
                .withSort(getSort(pageParameters));
        addFilterAggregations(nativeSearchQueryBuilder);
        SearchQuery searchQuery = nativeSearchQueryBuilder.build();
        Page<Board> responsePage = elasticsearchOperations.queryForPage(searchQuery, Board.class);
        Aggregations aggregations = elasticsearchOperations.query(searchQuery, SearchResponse::getAggregations);
        BoardResponse response = creatBoardResponse(responsePage);
        response.setFilters(getFilters(aggregations, searchFilters));
        return response;
    }

    private List<BoardFilter> getFilters(Aggregations aggregations, Map<String, String> searchFilters) {
        return aggregations.asMap().entrySet().stream()
                .map(entry -> new BoardFilter(entry.getKey(), getFilterValues(entry.getValue())))
                .peek(filter ->
                        filter.setSelectedValue(Optional.ofNullable(searchFilters.get(filter.getName()))
                                .orElse(StringUtil.EMPTY_STRING)))
                .collect(Collectors.toList());
    }

    private Map<String, Long> getFilterValues(Aggregation aggregation) {
        return ((Terms) aggregation).getBuckets().stream()
                .collect(Collectors.toMap(Terms.Bucket::getKeyAsString, Terms.Bucket::getDocCount));
    }

    private BoardResponse creatBoardResponse(Page<Board> responsePage) {
        BoardResponse response = new BoardResponse();
        response.setValues(responsePage.getContent());
        response.setPage(responsePage.getNumber());
        response.setPageSize(responsePage.getSize());
        response.setTotal(responsePage.getTotalElements());
        response.setTotalPages(responsePage.getTotalPages());
        return response;
    }

    private void addFilterAggregations(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        filterNames.forEach((filterName) ->
                nativeSearchQueryBuilder.addAggregation(AggregationBuilders
                        .terms(filterName)
                        .field(filterName)
                        .size(FILTER_SIZE)));
    }

    private FieldSortBuilder getSort(BoardPageParameters pageParameters) {
        return SortBuilders
                .fieldSort(pageParameters.getSortBy())
                .order(SortOrder.fromString(pageParameters.getSortOrder()));
    }

    private Pageable getPageable(BoardPageParameters pageParameters) {
        return PageRequest.of(pageParameters.getPage(), pageParameters.getPageSize());
    }

    private QueryBuilder getQueryForFilters(Map<String, String> searchFilters) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        searchFilters.forEach((key, value) ->
                boolQueryBuilder.must(QueryBuilders.termsQuery(key, value)));
        return boolQueryBuilder;
    }

    public BoardResponse searchBoards1(BoardPageParameters pageParameters) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        searchFields.forEach(field -> boolQueryBuilder.should(QueryBuilders.termsQuery(field, "Beginner")));
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(getPageable(pageParameters))
                .withSort(getSort(pageParameters));
        SearchQuery searchQuery = nativeSearchQueryBuilder.build();
        Page<Board> responsePage = elasticsearchOperations
                .queryForPage(searchQuery, Board.class);
        return creatBoardResponse(responsePage);
    }

    public BoardResponse searchBoards(BoardPageParameters pageParameters, String q) {
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(getQueryForSearchKey(q))
                .withPageable(getPageable(pageParameters))
                .withSort(getSort(pageParameters));
        SearchQuery searchQuery = nativeSearchQueryBuilder.build();
        Page<Board> responsePage = elasticsearchOperations.queryForPage(searchQuery, Board.class);
        return creatBoardResponse(responsePage);
    }

    private QueryBuilder getQueryForSearchKey(String key) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        searchFields.forEach(field -> boolQueryBuilder.should(QueryBuilders.fuzzyQuery(field, key)));
        return boolQueryBuilder;
    }
}
