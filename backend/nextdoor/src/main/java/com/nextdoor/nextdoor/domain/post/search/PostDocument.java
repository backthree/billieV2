package com.nextdoor.nextdoor.domain.post.search;

import com.nextdoor.nextdoor.domain.post.domain.Post;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Setting(settingPath = "/elasticsearch-settings.json")
@Document(indexName = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDocument {

  @Id
  private Long id;

  @MultiField(
          mainField = @Field(type = FieldType.Text, analyzer = "nori"),
          otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)}
  )
  private String title;

  @MultiField(
          mainField = @Field(type = FieldType.Text, analyzer = "nori"),
          otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)}
  )
  private String content;

  @Field(type = FieldType.Long)
  private Long rentalFee;

  @Field(type = FieldType.Long)
  private Long deposit;

  @MultiField(
          mainField = @Field(type = FieldType.Text, analyzer = "nori"),
          otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)}
  )
  private String address;

  @GeoPointField
  private GeoPoint location;

  @Field(type = FieldType.Keyword)
  private String category;

  @Field(type = FieldType.Long)
  private Long authorId;

  @Field(type = FieldType.Integer)
  private Integer likeCount;

  @Field(type = FieldType.Date)
  private Instant createdAt;

  public void copyFrom(Post post) {
    this.id = post.getId();
    this.title = post.getTitle();
    this.content = post.getContent();
    this.rentalFee = post.getRentalFee();
    this.deposit = post.getDeposit();
    this.address = post.getAddress();
    if (post.getLatitude() != null && post.getLongitude() != null) {
      this.location = new GeoPoint(post.getLatitude(), post.getLongitude());
    } else {
      this.location = null;
    }
    this.category = post.getCategory() != null ? post.getCategory().name() : null;
    this.authorId = post.getAuthorId();
    this.likeCount = post.getLikeCount();
    LocalDateTime ldt = post.getCreatedAt();
    if (ldt != null) {
      this.createdAt = ldt
              .atZone(ZoneId.systemDefault())
              .toInstant();
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GeoPoint {
    private Double lat;
    private Double lon;
  }
}