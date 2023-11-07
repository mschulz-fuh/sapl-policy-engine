/*
 * Copyright (C) 2017-2023 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.server.ce.persistence;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.sapl.server.ce.model.sapldocument.PublishedSaplDocument;

/**
 * Interface for a repository for accessing persisted
 * {@link PublishedSaplDocument}.
 */
@Repository
public interface PublishedSaplDocumentRepository extends CrudRepository<PublishedSaplDocument, Long>, Serializable {
	@Override
	Collection<PublishedSaplDocument> findAll();

	@Query(value = "SELECT s FROM PublishedSaplDocument s WHERE s.documentName = :documentName")
	Collection<PublishedSaplDocument> findByDocumentName(@Param(value = "documentName") String documentName);
}
