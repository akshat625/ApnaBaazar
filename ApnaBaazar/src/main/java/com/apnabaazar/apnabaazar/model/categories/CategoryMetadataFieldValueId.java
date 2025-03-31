package com.apnabaazar.apnabaazar.model.categories;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CategoryMetadataFieldValueId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String categoryId;
    private String categoryMetadataFieldId;

}
