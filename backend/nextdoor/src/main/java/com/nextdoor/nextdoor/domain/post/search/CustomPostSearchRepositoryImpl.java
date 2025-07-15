package com.nextdoor.nextdoor.domain.post.search;

import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch._types.FieldValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.support.PageableExecutionUtils;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.List;
import java.util.stream.Collectors;

import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;

@RequiredArgsConstructor
public class CustomPostSearchRepositoryImpl implements CustomPostSearchRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<PostDocument> searchByKeywordWithAddress(String keyword, String address, Pageable pageable) {

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (address != null && !address.isBlank()) {
            boolQuery.must(
                    new TermQuery.Builder()
                            .field("address.keyword")
                            .value(FieldValue.of(address))
                            .build()._toQuery()
            );
        }

        if (keyword != null && !keyword.isBlank()) {
            boolQuery.should(
                    new MatchQuery.Builder()
                            .field("title")
                            .query(FieldValue.of(keyword))
                            .operator(Operator.And)
                            .boost(3.0f)
                            .build()._toQuery()
            );

            boolQuery.should(
                    new MatchQuery.Builder()
                            .field("content")
                            .query(FieldValue.of(keyword))
                            .operator(Operator.And)
                            .boost(1.0f)
                            .build()._toQuery()
            );

            boolQuery.minimumShouldMatch("1");
        }

        Query query = boolQuery.build()._toQuery();

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable)
                .build();

        SearchHits<PostDocument> searchHits = elasticsearchOperations.search(
                searchQuery, PostDocument.class);

        List<PostDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return PageableExecutionUtils.getPage(content, pageable, searchHits::getTotalHits);
    }
}