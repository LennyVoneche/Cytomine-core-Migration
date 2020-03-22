package cytomine.core.utils

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import cytomine.core.CytomineDomain
import cytomine.core.security.SecUser

class CytomineService implements Serializable {

    static transactional = false
    def springSecurityService

    SecUser getCurrentUser() {
        return SecUser.read(springSecurityService.currentUser.id)
    }

    boolean isUserAlgo() {
        return getCurrentUser().algo()
    }

    public CytomineDomain getDomain(Long id,String className) {
        Class.forName(className, false, Thread.currentThread().contextClassLoader).read(id)
    }

}