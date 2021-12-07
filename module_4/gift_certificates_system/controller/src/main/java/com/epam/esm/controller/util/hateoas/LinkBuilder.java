package com.epam.esm.controller.util.hateoas;

import com.epam.esm.model.dto.AbstractDto;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface LinkBuilder<T extends AbstractDto<T>> {
    T build(T dto);

    Set<T> build(Set<T> dtos);
}
