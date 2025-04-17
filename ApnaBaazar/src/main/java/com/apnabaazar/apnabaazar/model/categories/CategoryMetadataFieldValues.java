package com.apnabaazar.apnabaazar.model.categories;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_metadata_field_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMetadataFieldValues {

    @EmbeddedId
    private CategoryMetadataFieldValueId id;

    @ManyToOne
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @MapsId("categoryMetadataFieldId")
    //use of
    @JoinColumn(name = "category_metadata_field_id")
    private CategoryMetadataField categoryMetadataField;

    @Column(name = "`values`")
    private String values;
}


