/*
 * Copyright 2024 Bloomreach (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scot.gov.publishing.reviewworkflow.repository.scxml;

import java.util.Set;

import javax.jcr.RepositoryException;

import scot.gov.publishing.reviewworkflow.repository.documentworkflow.ReviewWorkflowDocumentHandle;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

/**
 * Scxml task which fetches the currently logged in user's groups and set it on the workflowData
 */
public class ListGroupsTask extends AbstractDocumentTask {

    @Override
    protected Object doExecute() throws RepositoryException {
        final DocumentHandle documentHandle = getDocumentHandle();
        if (documentHandle instanceof ReviewWorkflowDocumentHandle) {
            final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
            final String userId = getWorkflowContext().getUserIdentity();
            if (!"system".equals(userId)){
                final User repoUser = securityService.getUser(userId);
                final Set<String> groups = repoUser.getMemberships();
                ((ReviewWorkflowDocumentHandle) documentHandle).setGroups(groups);
            }
        }
        return null;
    }

}
