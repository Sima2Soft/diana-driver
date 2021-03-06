/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jnosql.diana.orientdb.document;


import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordCallback;
import org.jnosql.diana.api.ExecuteAsyncQueryException;
import org.jnosql.diana.api.document.Document;
import org.jnosql.diana.api.document.DocumentCollectionManager;
import org.jnosql.diana.api.document.DocumentEntity;
import org.jnosql.diana.api.document.DocumentQuery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.orientechnologies.orient.core.db.ODatabase.OPERATION_MODE.ASYNCHRONOUS;

public class OrientDBDocumentCollectionManager implements DocumentCollectionManager {

    private static final Consumer<DocumentEntity> NOOPS = d -> {
    };
    private final OPartitionedDatabasePool pool;

    OrientDBDocumentCollectionManager(OPartitionedDatabasePool pool) {
        this.pool = pool;
    }

    @Override
    public DocumentEntity save(DocumentEntity entity) throws NullPointerException {
        Objects.toString(entity, "Entity is required");
        ODocument document = new ODocument(entity.getName());
        Map<String, Object> entityValues = entity.toMap();
        entityValues.keySet().stream().forEach(k -> document.field(k, entityValues.get(k)));
        ODatabaseDocumentTx tx = pool.acquire();
        tx.save(document);
        return entity;
    }


    @Override
    public DocumentEntity save(DocumentEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("There is support to ttl on OrientDB");
    }

    @Override
    public void saveAsync(DocumentEntity entity, Duration ttl) throws ExecuteAsyncQueryException, UnsupportedOperationException {
        throw new UnsupportedOperationException("There is support to ttl on OrientDB");
    }

    @Override
    public void saveAsync(DocumentEntity entity) throws ExecuteAsyncQueryException, UnsupportedOperationException {
        saveAsync(entity, NOOPS);
    }

    @Override
    public void saveAsync(DocumentEntity entity, Consumer<DocumentEntity> callBack) throws ExecuteAsyncQueryException, UnsupportedOperationException {
        Objects.toString(entity, "Entity is required");
        ODocument document = new ODocument(entity.getName());
        Map<String, Object> entityValues = entity.toMap();
        entityValues.keySet().stream().forEach(k -> document.field(k, entityValues.get(k)));
        ODatabaseDocumentTx tx = pool.acquire();
        ORecordCallback<Integer> oridentDBCallBack = (a, b) -> callBack.accept(entity);
        tx.save(document, null, ASYNCHRONOUS, false, oridentDBCallBack, oridentDBCallBack);
    }

    @Override
    public void saveAsync(DocumentEntity entity, Duration ttl, Consumer<DocumentEntity> callBack) throws ExecuteAsyncQueryException, UnsupportedOperationException {
        throw new UnsupportedOperationException("There is support to ttl on OrientDB");
    }

    @Override
    public DocumentEntity update(DocumentEntity entity) {
        return save(entity);
    }

    @Override
    public void updateAsync(DocumentEntity entity) throws ExecuteAsyncQueryException, UnsupportedOperationException {
        saveAsync(entity);
    }

    @Override
    public void updateAsync(DocumentEntity entity, Consumer<DocumentEntity> callBack) throws ExecuteAsyncQueryException, UnsupportedOperationException {
        saveAsync(entity, callBack);
    }

    @Override
    public void delete(DocumentQuery query) {
        ODatabaseDocumentTx tx = pool.acquire();
        List<ODocument> result = tx.query(OSQLQueryFactory.to(query));
        result.forEach(d -> tx.delete(d));

    }

    @Override
    public void deleteAsync(DocumentQuery query) throws ExecuteAsyncQueryException, UnsupportedOperationException {
        deleteAsync(query, v -> {
        });
    }

    @Override
    public void deleteAsync(DocumentQuery query, Consumer<Void> callBack) throws ExecuteAsyncQueryException, UnsupportedOperationException {
        ODatabaseDocumentTx tx = pool.acquire();
        tx.command(OSQLQueryFactory.toAsync(query, callBack));
    }

    @Override
    public List<DocumentEntity> find(DocumentQuery query) throws NullPointerException {
        ODatabaseDocumentTx tx = pool.acquire();
        List<ODocument> result = tx.query(OSQLQueryFactory.to(query));
        List<DocumentEntity> entities = new ArrayList<>();
        for (ODocument document : result) {
            DocumentEntity entity = DocumentEntity.of(query.getCollection());
            Stream.of(document.fieldNames())
                    .map(f -> Document.of(f, document.field(f)))
                    .forEach(entity::add);
            entities.add(entity);
        }
        return entities;
    }

    @Override
    public void findAsync(DocumentQuery query, Consumer<List<DocumentEntity>> callBack) throws ExecuteAsyncQueryException, UnsupportedOperationException {

    }

    @Override
    public void close() {
        pool.close();
    }
}
