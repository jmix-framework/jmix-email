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

package email_cleaner

import io.jmix.core.Metadata
import io.jmix.core.SaveContext
import io.jmix.core.TimeSource
import io.jmix.core.UnconstrainedDataManager
import io.jmix.email.EmailCleaner
import io.jmix.email.entity.SendingAttachment
import io.jmix.email.entity.SendingMessage
import org.springframework.beans.factory.annotation.Autowired
import test_support.EmailSpecification

class EmailCleanerTest extends EmailSpecification {
    @Autowired
    EmailCleaner emailCleaner

    @Autowired
    Metadata metadata

    @Autowired
    UnconstrainedDataManager dataManager

    @Autowired
    TimeSource timeSource

    def setup() {
        prepareTestData()
    }

    def 'Delete old messages with important messages age = 1 and usual messages age = 1'() {
        when: 'Nothing to delete'
        def amountOfDeletedMessages = emailCleaner.deleteOldEmails()

        then:
        amountOfDeletedMessages == 0

        when: 'All important messages created yesterday'
        def importantMessagesToDelete = loadAllSendingMessages().stream()
                .filter(x -> x.important)
                .peek(x -> x.setCreateTs(Date.from(timeSource.now().minusHours(25).toInstant())))
                .toArray()
        dataManager.save(importantMessagesToDelete)
        amountOfDeletedMessages = emailCleaner.deleteOldEmails()

        then: 'All important messages and their attachments should be deleted'
        amountOfDeletedMessages == 5
        loadAllSendingMessages().stream().allMatch(x -> !x.important)

        when: 'All messages created yesterday'
        def messagesToDelete = loadAllSendingMessages().stream()
                .peek(x -> x.setCreateTs(Date.from(timeSource.now().minusHours(25).toInstant())))
                .toArray()
        dataManager.save(messagesToDelete)
        amountOfDeletedMessages = emailCleaner.deleteOldEmails()

        then: 'All messages and their attachments should be deleted'
        amountOfDeletedMessages == 5
        loadAllSendingMessages().isEmpty()
    }

    private List<SendingMessage> loadAllSendingMessages() {
        return dataManager.load(SendingMessage).all().list()
    }

    private void prepareTestData() {
        def entitiesToSave = []

        SendingMessage message1 = metadata.create(SendingMessage)
        entitiesToSave << message1
        3.times { entitiesToSave << createSendingAttachment(message1) }

        SendingMessage message2 = metadata.create(SendingMessage)
        message2.important = true
        entitiesToSave << message2
        3.times { entitiesToSave << createSendingAttachment(message2) }

        SendingMessage message3 = metadata.create(SendingMessage)
        entitiesToSave << message3

        SendingMessage message4 = metadata.create(SendingMessage)
        message4.important = true
        entitiesToSave << message4

        dataManager.save(new SaveContext().saving(entitiesToSave))
    }

    private SendingAttachment createSendingAttachment(SendingMessage message) {
        SendingAttachment attachment = metadata.create(SendingAttachment)
        attachment.message = message
        return attachment
    }
}
