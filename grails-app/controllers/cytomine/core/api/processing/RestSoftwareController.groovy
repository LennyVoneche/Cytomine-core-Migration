package cytomine.core.api.processing

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

import cytomine.core.api.RestController
import cytomine.core.processing.Job
import cytomine.core.processing.Software
import cytomine.core.processing.SoftwareUserRepository
import cytomine.core.project.Project
import grails.converters.JSON
//import org.restapidoc.annotation.*
//import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for software: application that can be launch (job)
 */
//@RestApi(name = "Processing | software services", description = "Methods for managing software, application that can be launch (job)")
class RestSoftwareController extends RestController {

    def softwareService

    /**
     * List all software available in cytomine
     */
//    @RestApiMethod(description="Get all software available in cytomine", listing = true)
    def list() {
        boolean executableOnly = params.boolean('executableOnly', false)
        String sort = params.sort ?: 'id'
        if (!['id', 'name', 'fullName', 'softwareVersion', 'created'].contains(sort)) sort = 'id'
        String order = params.order ?: 'desc'
        if (!['asc', 'desc'].contains(order)) order = 'desc'
        responseSuccess(softwareService.list(executableOnly, sort, order))
    }

    /**
     * List all software by project
     */
//    @RestApiMethod(description="Get all software available in a project", listing = true)
//    @RestApiParams(params=[
//        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
//    ])
    def listByProject() {
        Project project = Project.read(params.long('id'))
        if(project) responseSuccess(softwareService.list(project))
        else responseNotFound("Project", params.id)
    }

    /**
     * List all software by software user repository
     */
//    @RestApiMethod(description = "Get all the software for a software use repository", listing = true)
//    @RestApiParams(params = [
//        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software user repository id")
//    ])
    def listBySoftwareUserRepository() {
        SoftwareUserRepository softwareUserRepository = SoftwareUserRepository.read(params.long('id'))
        if (softwareUserRepository) {
            responseSuccess(softwareService.list(softwareUserRepository))
        } else {
            responseNotFound("SoftwareUserRepository", params.id)
        }
    }

    /**
     * Get a specific software
     */
//    @RestApiMethod(description="Get a specific software")
//    @RestApiParams(params=[
//        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software id")
//    ])
    def show() {
        Software software = softwareService.read(params.long('id'))
        if (software) {
            responseSuccess(software)
        } else {
            responseNotFound("Software", params.id)
        }
    }

    /**
     * Add a new software to cytomine
     * We must add in other request: parameters, software-project link,...
     */
//    @RestApiMethod(description="Add a new software to cytomine. We must add in other request: software parameters, software project link,...")
    def add() {
        add(softwareService, request.JSON)
    }

    /**
     * Update a software info
     */
//    @RestApiMethod(description="Update a software.", listing = true)
//    @RestApiParams(params=[
//        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software id")
//    ])
    def update() {
        update(softwareService, request.JSON)
    }

    /**
     * Delete software
     */
//    @RestApiMethod(description="Delete a software.", listing = true)
//    @RestApiParams(params=[
//        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The software id")
//    ])
    def delete() {
        delete(softwareService, JSON.parse("{id : $params.id}"),null)
    }

    /**
     * List software
     * TODO:: could be improved with a single SQL request
     *
     */
//    @RestApiMethod(description="For a software and a project, get the stats (number of job, succes,...)", listing = true)
//    @RestApiParams(params=[
//        @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
//        @RestApiParam(name="idSoftware", type="long", paramType = RestApiParamType.PATH, description = "The software id"),
//    ])
//    @RestApiResponseObject(objectIdentifier = "[numberOfJob:x,numberOfNotLaunch:x,numberOfInQueue:x,numberOfRunning:x,numberOfSuccess:x,numberOfFailed:x,numberOfIndeterminate:x,numberOfWait:x]")
    def softwareInfoForProject() {
        Project project = Project.read(params.long('idProject'))
        Software software = Software.read(params.long('idSoftware'))
        if(!project) {
            responseNotFound("Project", params.idProject)
        } else if(!software) {
            responseNotFound("Software", params.idSoftware)
        } else {
            def result = [:]
            List<Job> jobs = Job.findAllByProjectAndSoftware(project,software)
            
            //Number of job for this software and this project
            result['numberOfJob'] = jobs.size()
            
            //Number of job by state
            result['numberOfNotLaunch'] = 0
            result['numberOfInQueue'] = 0
            result['numberOfRunning'] = 0
            result['numberOfSuccess'] = 0
            result['numberOfFailed'] = 0
            result['numberOfIndeterminate'] = 0
            result['numberOfWait'] = 0
            
            jobs.each { job ->
                if(job.status==Job.NOTLAUNCH) result['numberOfNotLaunch']++
                if(job.status==Job.INQUEUE) result['numberOfInQueue']++
                if(job.status==Job.RUNNING) result['numberOfRunning']++
                if(job.status==Job.SUCCESS) result['numberOfSuccess']++
                if(job.status==Job.FAILED) result['numberOfFailed']++
                if(job.status==Job.INDETERMINATE) result['numberOfIndeterminate']++
                if(job.status==Job.WAIT) result['numberOfWait']++
            }

            responseSuccess(result)
        }
    }
}
