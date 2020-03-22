package cytomine.core.image

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
import cytomine.core.image.server.MimeImageServer

/**
 * Image Extension
 */
class Mime extends CytomineDomain implements Serializable {

    String extension
    String mimeType

    static constraints = {
        extension(maxSize: 5, blank: false, unique: false)
        mimeType(blank: false, unique: true)
    }

    static mapping = {
        id(generator: 'assigned', unique: true)
        sort "id"
        cache true
    }

    /**
     * Get list of image server that support this mime
     * @return Image server list
     */
    def imageServers() {
        MimeImageServer.findAllByMime(this).collect {it.imageServer}
    }

    String toString() {
        mimeType
    }

}