package com.group3.vitamins.file.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** {@code block_file} 복합 PK ({@code block_id} + {@code file_id})용 식별자 클래스. */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BlockFileId implements Serializable {

    private Long blockId;
    private Long fileId;
}
