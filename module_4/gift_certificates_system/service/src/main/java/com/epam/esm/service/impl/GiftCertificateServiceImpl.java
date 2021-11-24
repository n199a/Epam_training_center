package com.epam.esm.service.impl;

import com.epam.esm.dao.TagDao;
import com.epam.esm.dao.impl.GiftCertificateToTagRelationDaoImpl;
import com.epam.esm.dto.GiftCertificateDto;
import com.epam.esm.dto.GiftCertificateUpdateDto;
import com.epam.esm.dto.TagDto;
import com.epam.esm.entity.GiftCertificate;
import com.epam.esm.entity.GiftCertificateToTagRelation;
import com.epam.esm.entity.Tag;
import com.epam.esm.exception.EntityExistingException;
import com.epam.esm.exception.EntityNonExistentException;
import com.epam.esm.pojo.GiftCertificateSearchParameter;
import com.epam.esm.repository.GiftCertificateRepository;
import com.epam.esm.service.GitCertificateService;
import com.epam.esm.util.EsmPagination;
import com.epam.esm.util.GiftCertificateFieldChecker;
import com.epam.esm.util.GiftCertificateUpdater;
import com.epam.esm.util.PageMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.esm.util.MessagePropertyKey.EXCEPTION_GIFT_CERTIFICATE_ID_NOT_FOUND;
import static com.epam.esm.util.MessagePropertyKey.EXCEPTION_GIFT_CERTIFICATE_NAME_EXISTS;
import static com.epam.esm.util.MessagePropertyKey.EXCEPTION_GIFT_CERTIFICATE_UPDATE_FIELDS_EMPTY;

/**
 * Gift certificate service implementation.
 */
@Log4j2
@Service
@Transactional
public class GiftCertificateServiceImpl implements GitCertificateService {
    private final TagDao tagDao;
    private final GiftCertificateRepository certificateRepository;
    private final GiftCertificateToTagRelationDaoImpl relationDao;
    private final ModelMapper modelMapper;
    private final PageMapper pageMapper;

    @Autowired
    public GiftCertificateServiceImpl(GiftCertificateRepository certificateRepository, TagDao tagDao, GiftCertificateToTagRelationDaoImpl relationDao, ModelMapper modelMapper, PageMapper pageMapper) {
        this.certificateRepository = certificateRepository;
        this.tagDao = tagDao;
        this.relationDao = relationDao;
        this.modelMapper = modelMapper;
        this.pageMapper = pageMapper;
    }

    @Override
    public GiftCertificateDto create(GiftCertificateDto certificateDto) {
        checkIfNonExistsOrElseThrow(certificateDto);

        GiftCertificate certificate = modelMapper.map(certificateDto, GiftCertificate.class);
        GiftCertificate createdCertificate = certificateRepository.save(certificate);

        certificateDto.getTags().stream()
                .map(tagDto -> modelMapper.map(tagDto, Tag.class))
                .map(tag -> tagDao.findByName(tag.getName()).orElseGet(() -> tagDao.create(tag)))
                .map(tag -> new GiftCertificateToTagRelation(createdCertificate, tag))
                .forEach(relation -> {
                    if (!relationDao.findBy(relation).isPresent()) {
                        relationDao.create(relation);
                    }
                });

        GiftCertificateDto gcDto = modelMapper.map(createdCertificate, GiftCertificateDto.class);
        Set<TagDto> tagsDto = tagDao.findAllBy(createdCertificate).stream()
                .map(tag -> modelMapper.map(tag, TagDto.class))
                .collect(Collectors.toSet());
        gcDto.setTags(tagsDto);

        return gcDto;
    }

    @Override
    public GiftCertificateDto findById(long id) {
        GiftCertificate gc = certificateRepository.findById(id)
                .orElseThrow(() -> new EntityNonExistentException(EXCEPTION_GIFT_CERTIFICATE_ID_NOT_FOUND, id));
        return buildGiftCertificateDtoWithTags(gc);
    }

    @Override
    public Page<GiftCertificateDto> findAll(EsmPagination pagination) {
        Pageable pageable = pageMapper.map(pagination);
        Page<GiftCertificate> certificates = certificateRepository.findAll(pageable);
        return pageMapper.map(certificates, GiftCertificateDto.class);

        // fixme remove
//        Set<GiftCertificateDto> certificates = certificateRepository.findAll(pageable).stream()
//                .map(this::buildGiftCertificateDtoWithTags)
//                .collect(Collectors.toSet());
//
//        return PageMapper.map(certificates);
    }

    // todo
    @Override
    public Page<GiftCertificateDto> findAll(EsmPagination pagination, GiftCertificateSearchParameter searchParameter) {
        Pageable pageable = pageMapper.map(pagination);
        Page<GiftCertificate> certificates = certificateRepository.findAll(searchParameter, pageable);
        return pageMapper.map(certificates, GiftCertificateDto.class);

        // fixme remove
//        if (ObjectUtils.isNotEmpty(certificates)) {
//            return certificates.stream()
//                    .map(certificate -> modelMapper.map(certificate, GiftCertificateDto.class))
//                    .collect(Collectors.toSet());
//        } else {
//            throw new EntityNotFoundException(EXCEPTION_GIFT_CERTIFICATE_WITH_SEARCH_PARAMETERS);
//        }
    }

    @Override
    public GiftCertificateDto update(long id, GiftCertificateUpdateDto certificate) {
        GiftCertificate foundGc = certificateRepository.findById(id)
                .orElseThrow(() -> new EntityNonExistentException(EXCEPTION_GIFT_CERTIFICATE_ID_NOT_FOUND, id));

        if (GiftCertificateFieldChecker.isFilledOneField(certificate)) {
            GiftCertificate updatedGc = modelMapper.map(certificate, GiftCertificate.class);

            GiftCertificate updated = GiftCertificateUpdater.update(foundGc, updatedGc);
            GiftCertificate created = certificateRepository.save(updated);  // update

            if (ObjectUtils.isNotEmpty(certificate.getTags())) {
                updateRelations(created, certificate);
            }

            return buildGiftCertificateDtoWithTags(created);
        } else {
            throw new IllegalArgumentException(EXCEPTION_GIFT_CERTIFICATE_UPDATE_FIELDS_EMPTY);
        }
    }

    @Override
    public void delete(long id) {
        Optional<GiftCertificate> optionalCertificate = certificateRepository.findById(id);
        GiftCertificate gc = optionalCertificate.orElseThrow(() -> new EntityNonExistentException(EXCEPTION_GIFT_CERTIFICATE_ID_NOT_FOUND, id));
        delete(gc);
    }

    private void checkIfNonExistsOrElseThrow(GiftCertificateDto certificate) {
        String name = certificate.getName();
        Optional<GiftCertificate> optionalGiftCertificate = certificateRepository.findByName(name);

        if (optionalGiftCertificate.isPresent()) {
            throw new EntityExistingException(EXCEPTION_GIFT_CERTIFICATE_NAME_EXISTS, name);
        }
    }

    private void updateRelations(GiftCertificate certificate, GiftCertificateUpdateDto updatedCertificate) {
        Set<Tag> tagsFromRequest = findTagsBy(updatedCertificate);
        tagsFromRequest.forEach(tag -> createRelationIfNonExist(certificate, tag));
        deleteIrrelevantRelations(certificate, tagsFromRequest);
    }

    private void createRelationIfNonExist(GiftCertificate certificate, Tag tag) {
        String tagName = tag.getName();
        Optional<Tag> optionalTag = tagDao.findByName(tagName);

        if (optionalTag.isPresent()) {
            Tag foundTag = optionalTag.get();
            boolean isExistRelation = relationDao.isExist(certificate, foundTag);

            if (!isExistRelation) {
                GiftCertificateToTagRelation relation = new GiftCertificateToTagRelation(certificate, foundTag);
                relationDao.create(relation);
            }
        } else {
            Tag createdTag = tagDao.create(tag);
            GiftCertificateToTagRelation relation = new GiftCertificateToTagRelation(certificate, createdTag);
            relationDao.create(relation);
        }
    }

    private Set<Tag> findTagsBy(GiftCertificate certificate) {
        return tagDao.findAllBy(certificate);
    }

    private Set<TagDto> findTagsDtoBy(GiftCertificate certificate) {
        return findTagsBy(certificate).stream()
                .map(tag -> modelMapper.map(tag, TagDto.class))
                .collect(Collectors.toSet());
    }

    private Set<Tag> findTagsBy(GiftCertificateUpdateDto certificate) {
        return certificate.getTags().stream()
                .map(tagDto -> modelMapper.map(tagDto, Tag.class))
                .collect(Collectors.toSet());
    }

    private GiftCertificateDto buildGiftCertificateDtoWithTags(GiftCertificate certificate) {
        Set<TagDto> tagsDto = findTagsDtoBy(certificate);
        GiftCertificateDto gcDto = modelMapper.map(certificate, GiftCertificateDto.class);
        gcDto.setTags(tagsDto);
        return gcDto;
    }

    private Set<GiftCertificateDto> buildGiftCertificatesDtoWithTags(Set<GiftCertificate> certificates) {
        return certificates.stream()
                .map(this::buildGiftCertificateDtoWithTags)
                .collect(Collectors.toSet());
    }

    private void delete(GiftCertificate certificate) {
        deleteRelations(certificate);
        certificateRepository.delete(certificate);
    }

    private void deleteRelations(GiftCertificate certificate) {
        relationDao.findAllBy(certificate)
                .forEach(relationDao::delete);
    }

    private void deleteIrrelevantRelations(GiftCertificate certificate, Set<Tag> tagsFromRequest) {
        Set<Tag> tagsFromDatabase = findTagsBy(certificate);
        Set<Tag> tagForRemove = new HashSet<>(tagsFromDatabase);
        tagForRemove.removeAll(tagsFromRequest);
        tagForRemove.stream()
                .map(tag -> new GiftCertificateToTagRelation(certificate, tag))
                .forEach(relationDao::delete);
    }
}
