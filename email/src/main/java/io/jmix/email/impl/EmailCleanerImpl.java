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
import io.jmix.data.PersistenceHints;
import io.jmix.email.EmailCleaner;
import io.jmix.email.EmailerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Objects;


@Component("email_EmailCleaner")
public class EmailCleanerImpl implements EmailCleaner {

    private static final String PATH_TO_SQL_SCRIPT = "classpath:/io/jmix/email/script/deleteSendingMessages.sql";

    @Autowired
    private EmailerProperties emailerProperties;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private Resources resources;

    @Transactional
    @Override
    public Integer deleteOldEmails() {
        int maxAgeOfImportantMessages = emailerProperties.getMaxAgeOfImportantMessages();
        int maxAgeOfNonImportantMessages = emailerProperties.getMaxAgeOfNonImportantMessages();
        entityManager.setProperty(PersistenceHints.SOFT_DELETION, false);

        StringBuilder queryStringBuilder = new StringBuilder();
        String sqlScript = Objects.requireNonNull(resources.getResourceAsString(PATH_TO_SQL_SCRIPT));
        String deleteQueryForNonImportantMessages = sqlScript
                .replace("{days}", String.valueOf(maxAgeOfNonImportantMessages))
                .replace("{important}", "false");
        queryStringBuilder.append(deleteQueryForNonImportantMessages).append("\n");

        if (maxAgeOfImportantMessages != 0) {
            String deleteQueryForImportantMessages = sqlScript
                    .replace("{days}", String.valueOf(maxAgeOfImportantMessages))
                    .replace("{important}", "true");
            queryStringBuilder.append(deleteQueryForImportantMessages);
        }

        return entityManager.createNativeQuery(queryStringBuilder.toString()).executeUpdate();
    }
}