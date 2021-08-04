/*
 * Copyright 2021 Haulmont.
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

package io.jmix.email.impl;

import io.jmix.core.Resources;
import io.jmix.core.TimeSource;
import io.jmix.data.PersistenceHints;
import io.jmix.email.EmailCleaner;
import io.jmix.email.EmailerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.stream.Collectors;


@Component("email_EmailCleaner")
public class EmailCleanerImpl implements EmailCleaner {

    private static final String PATH_TO_SQL_SCRIPT = "classpath:/io/jmix/email/script/deleteSendingMessages.sql";

    @Autowired
    private EmailerProperties emailerProperties;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private Resources resources;

    @Autowired
    private TimeSource timeSource;

    @Transactional
    @Override
    public Integer deleteOldEmails() {
        int maxAgeOfImportantMessages = emailerProperties.getMaxAgeOfImportantMessages();
        int maxAgeOfNonImportantMessages = emailerProperties.getMaxAgeOfNonImportantMessages();
        entityManager.setProperty(PersistenceHints.SOFT_DELETION, false);

        StringBuilder queryStringBuilder = new StringBuilder();
        String nativeDeleteSqlScript = Objects.requireNonNull(resources.getResourceAsString(PATH_TO_SQL_SCRIPT));
        if (maxAgeOfNonImportantMessages != 0) {
            List<UUID> ids = entityManager.createQuery("select msg.id from email_SendingMessage msg" +
                    " where msg.important = false and msg.createTs < :date", UUID.class)
                    .setParameter("date", Date.from(timeSource.now().minusDays(maxAgeOfNonImportantMessages).toInstant()))
                    .getResultList();
            if (!ids.isEmpty()) {
                String deleteQueryForNonImportantMessages = nativeDeleteSqlScript.replace("{placeHolders}",
                        ids.stream().map(id -> "'" + id.toString() + "'").collect(Collectors.joining(",")));
                queryStringBuilder.append(deleteQueryForNonImportantMessages).append("\n");
            }
        }

        if (maxAgeOfImportantMessages != 0) {
            List<UUID> ids = entityManager.createQuery("select msg.id from email_SendingMessage msg" +
                    " where msg.important = true and msg.createTs < :date", UUID.class)
                    .setParameter("date", Date.from(timeSource.now().minusDays(maxAgeOfImportantMessages).toInstant()))
                    .getResultList();
            if (!ids.isEmpty()) {
                String deleteQueryForImportantMessages = nativeDeleteSqlScript.replace("{placeHolders}",
                        ids.stream().map(id -> "'" + id.toString() + "'").collect(Collectors.joining(",")));
                queryStringBuilder.append(deleteQueryForImportantMessages);
            }
        }

        String queryString = queryStringBuilder.toString();
        if (queryString.isEmpty()) {
            return 0;
        } else {
            return entityManager.createNativeQuery(queryString).executeUpdate();
        }
    }
}
