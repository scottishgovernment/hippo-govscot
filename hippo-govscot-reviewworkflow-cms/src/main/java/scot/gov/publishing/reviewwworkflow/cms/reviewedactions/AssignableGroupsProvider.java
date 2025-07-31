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
package scot.gov.publishing.reviewwworkflow.cms.reviewedactions;

import java.util.Set;

/**
 * Interface for dynamically providing assignable group names in the internal assign dialog's dropdown
 */
public interface AssignableGroupsProvider {
    /**
     * @param currentUserId Currently logged in user's user id
     * @param docAbsolutePath Absolute path of the document for which a review request is being asked
     * @return Set of group Ids
     */
    Set<String> provideGroups(final String currentUserId, String docAbsolutePath);
}
